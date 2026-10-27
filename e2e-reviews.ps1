# Live end-to-end test for reviews / ratings.
# Requires the backend running on http://localhost:9090
# Usage:  powershell -ExecutionPolicy Bypass -File .\e2e-reviews.ps1

$ErrorActionPreference = 'Stop'
$base = 'http://localhost:9090/api'
$stamp = Get-Date -Format 'HHmmss'

function Post-Json($url, $body, $token) {
    $headers = @{ 'Content-Type' = 'application/json' }
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body ($body | ConvertTo-Json -Depth 5)
}

function Put-Json($url, $token) {
    $headers = @{ 'Authorization' = "Bearer $token" }
    Invoke-RestMethod -Uri $url -Method Put -Headers $headers
}

function Get-Json($url, $token) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    Invoke-RestMethod -Uri $url -Headers $headers
}

$results = @()
function Check($name, $condition) {
    if ($condition) {
        Write-Host "  [PASS] $name" -ForegroundColor Green
        $script:results += $true
    } else {
        Write-Host "  [FAIL] $name" -ForegroundColor Red
        $script:results += $false
    }
}

Write-Host "`n=== SkillSwap reviews e2e test ===`n" -ForegroundColor Cyan

$health = Get-Json "$base/health"
Check "Health endpoint returns UP" ($health.data.status -eq 'UP')

# --- Setup: two users, one shared learning session, one swap ---
$teacher = Post-Json "$base/auth/register" @{
    name = "Rita Reviewer"; username = "rita_e2e_$stamp"; email = "rita_e2e_$stamp@test.com"
    password = "secret123"; college = "Test College"; branch = "CSE"; year = "4th Year"; mode = "Both"
}
$learner = Post-Json "$base/auth/register" @{
    name = "Sam Student"; username = "sam_e2e_$stamp"; email = "sam_e2e_$stamp@test.com"
    password = "secret123"; college = "Test College"; branch = "IT"; year = "2nd Year"; mode = "Both"
}
$teacherToken = $teacher.data.accessToken
$learnerToken = $learner.data.accessToken
$teacherId = $teacher.data.user.id
$learnerId = $learner.data.user.id

$skills = @((Get-Json "$base/skills").data)
$python = $skills | Where-Object { $_.name -eq 'Python' } | Select-Object -First 1
$react  = $skills | Where-Object { $_.name -eq 'React' }  | Select-Object -First 1

Post-Json "$base/users/skills" @{ skillId = $python.id; skillType = "TEACH"; proficiency = "ADVANCED" } $teacherToken | Out-Null
Post-Json "$base/users/skills" @{ skillId = $react.id;  skillType = "TEACH"; proficiency = "INTERMEDIATE" } $learnerToken | Out-Null
Post-Json "$base/users/skills" @{ skillId = $react.id;  skillType = "LEARN"; proficiency = "BEGINNER" } $teacherToken | Out-Null

# --- 1. A pending (not completed) session cannot be reviewed ---
$req = Post-Json "$base/learning" @{ teacherId = $teacherId; skillId = $python.id } $learnerToken
$reqId = $req.data.id
$earlyReviewRejected = $false
try {
    Post-Json "$base/reviews" @{ learningRequestId = $reqId; rating = 5; comment = "Too early" } $learnerToken | Out-Null
} catch { $earlyReviewRejected = $true }
Check "Cannot review an uncompleted session" $earlyReviewRejected

# --- 2. Complete the session, then review ---
Put-Json "$base/learning/$reqId/accept" $teacherToken | Out-Null
Put-Json "$base/learning/$reqId/complete" $learnerToken | Out-Null

$review = Post-Json "$base/reviews" @{
    learningRequestId = $reqId; rating = 5; comment = "Rita explained Python clearly. Highly recommend!"
} $learnerToken
Check "Learner can review a completed session" ($review.data.rating -eq 5)
Check "Review records the reviewee" ($review.data.revieweeId -eq $teacherId)
Check "Review records the context type" ($review.data.contextType -eq 'LEARNING')
Check "Review records the skill" ($review.data.skillName -eq 'Python')
Check "Review hides the reviewer's email" (-not ($review.data.reviewer.PSObject.Properties.Name -contains 'email'))

# --- 3. Duplicate review rejected ---
$dupReviewRejected = $false
try {
    Post-Json "$base/reviews" @{ learningRequestId = $reqId; rating = 1 } $learnerToken | Out-Null
} catch { $dupReviewRejected = $true }
Check "Duplicate review rejected" $dupReviewRejected

# --- 4. Rating bounds enforced ---
$zeroRejected = $false
try {
    Post-Json "$base/reviews" @{ learningRequestId = $reqId; rating = 0 } $learnerToken | Out-Null
} catch { $zeroRejected = $true }
Check "Rating below 1 rejected" $zeroRejected

# --- 5. Outsider cannot review someone else's session ---
$outsider = Post-Json "$base/auth/register" @{
    name = "Otto Outsider"; username = "otto_e2e_$stamp"; email = "otto_e2e_$stamp@test.com"
    password = "secret123"; college = "Test College"; branch = "ECE"; year = "1st Year"; mode = "Both"
}
$outsiderToken = $outsider.data.accessToken
$outsiderRejected = $false
try {
    Post-Json "$base/reviews" @{ learningRequestId = $reqId; rating = 5 } $outsiderToken | Out-Null
} catch { $outsiderRejected = $true }
Check "Outsider cannot review the session" $outsiderRejected

# --- 6. Pending reviews list is now empty for the learner ---
$pending = @((Get-Json "$base/reviews/pending" $learnerToken).data)
Check "Learner has no pending reviews left" (@($pending | Where-Object { $_.type -eq 'LEARNING' -and $_.contextId -eq $reqId }).Count -eq 0)

# --- 7. Teacher has a pending review for the same session ---
$teacherPending = @((Get-Json "$base/reviews/pending" $teacherToken).data)
$teacherItem = $teacherPending | Where-Object { $_.type -eq 'LEARNING' -and $_.contextId -eq $reqId } | Select-Object -First 1
Check "Teacher is prompted to review the session" ($null -ne $teacherItem)
Check "Pending item names the counterpart" ($teacherItem.counterpartName -eq 'Sam Student')

$teacherReview = Post-Json "$base/reviews" @{ learningRequestId = $reqId; rating = 4; comment = "Great learner, asked good questions." } $teacherToken
Check "Teacher can review the learner too" ($teacherReview.data.revieweeId -eq $learnerId)

# --- 8. Ratings aggregate into the public profile ---
$profile = Get-Json "$base/users/$teacherId/profile"
Check "Public profile exposes avgRating" ($profile.data.avgRating -eq 5)
Check "Public profile exposes reviewCount" ($profile.data.reviewCount -eq 1)

$learnerProfile = Get-Json "$base/users/$learnerId/profile"
Check "Learner profile aggregates their rating" ($learnerProfile.data.avgRating -eq 4)

# --- 9. Reviews are publicly readable ---
$publicReviews = @((Get-Json "$base/reviews/user/$teacherId").data)
Check "Reviews readable without a token" ($publicReviews.Count -eq 1)
Check "Public review has the right comment" ($publicReviews[0].comment -like 'Rita explained*')

# --- 10. Marketplace shows the rating ---
$market = @((Get-Json "$base/marketplace?query=Python").data)
$listing = $market | Where-Object { $_.teacherId -eq $teacherId } | Select-Object -First 1
Check "Marketplace listing carries avgRating" ($listing.avgRating -eq 5)
Check "Marketplace listing carries reviewCount" ($listing.reviewCount -eq 1)
Check "Marketplace listing carries the college" ($listing.teacherCollege -eq 'Test College')

# --- 11. Swap reviews work the same way ---
$swap = Post-Json "$base/swaps" @{
    receiverId = $teacherId; offeredSkillId = $react.id; requestedSkillId = $python.id
} $learnerToken
$swapId = $swap.data.id

$swapEarlyRejected = $false
try {
    Post-Json "$base/reviews" @{ swapId = $swapId; rating = 5 } $learnerToken | Out-Null
} catch { $swapEarlyRejected = $true }
Check "Cannot review an incomplete swap" $swapEarlyRejected

Put-Json "$base/swaps/$swapId/accept" $teacherToken | Out-Null
Put-Json "$base/swaps/$swapId/complete" $learnerToken | Out-Null

$swapReview = Post-Json "$base/reviews" @{ swapId = $swapId; rating = 3; comment = "Decent swap." } $learnerToken
Check "Swap can be reviewed once completed" ($swapReview.data.contextType -eq 'SWAP')

$teacherProfile = Get-Json "$base/users/$teacherId/profile"
Check "Average blends learning + swap reviews" ($teacherProfile.data.avgRating -eq 4)
Check "Review count reflects both" ($teacherProfile.data.reviewCount -eq 2)

# --- 12. Exactly one target is required ---
$bothRejected = $false
try {
    Post-Json "$base/reviews" @{ swapId = $swapId; learningRequestId = $reqId; rating = 5 } $teacherToken | Out-Null
} catch { $bothRejected = $true }
Check "Both targets rejected" $bothRejected

$neitherRejected = $false
try {
    Post-Json "$base/reviews" @{ rating = 5 } $teacherToken | Out-Null
} catch { $neitherRejected = $true }
Check "Missing target rejected" $neitherRejected

# --- 13. Auth required to write a review ---
$unauthRejected = $false
try {
    Invoke-WebRequest -Uri "$base/reviews/pending" -UseBasicParsing -TimeoutSec 5 | Out-Null
} catch { $unauthRejected = $true }
Check "Pending reviews require authentication" $unauthRejected

$passed = @($results | Where-Object { $_ }).Count
$total = @($results).Count
Write-Host "`n=== $passed / $total checks passed ===`n" -ForegroundColor $(if ($passed -eq $total) { 'Green' } else { 'Red' })
if ($passed -ne $total) { exit 1 }
