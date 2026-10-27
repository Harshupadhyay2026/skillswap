# Live end-to-end test for the Q&A (problems) module.
# Requires the backend running on http://localhost:9090
# Usage:  powershell -ExecutionPolicy Bypass -File .\e2e-qa.ps1

$ErrorActionPreference = 'Stop'
$base = 'http://localhost:9090/api'
$stamp = Get-Date -Format 'HHmmss'

function Post-Json($url, $body, $token) {
    $headers = @{ 'Content-Type' = 'application/json' }
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body ($body | ConvertTo-Json -Depth 5)
}

function Put-Json($url, $token) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
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

Write-Host "`n=== SkillSwap Q&A e2e test ===`n" -ForegroundColor Cyan

$health = Get-Json "$base/health"
Check "Health endpoint returns UP" ($health.data.status -eq 'UP')

# --- Setup: an asker and a helper ---
$asker = Post-Json "$base/auth/register" @{
    name = "Ayana Asker"; username = "ayana_qa_e2e_$stamp"; email = "ayana_qa_e2e_$stamp@test.com"
    password = "secret123"; college = "Test College"; branch = "CSE"; year = "2nd Year"; mode = "Both"
}
$helper = Post-Json "$base/auth/register" @{
    name = "Dev Helper"; username = "dev_qa_e2e_$stamp"; email = "dev_qa_e2e_$stamp@test.com"
    password = "secret123"; college = "Test College"; branch = "IT"; year = "3rd Year"; mode = "Both"
}
$askerToken = $asker.data.accessToken
$helperToken = $helper.data.accessToken
$askerId = $asker.data.user.id
$helperId = $helper.data.user.id

# --- 1. Reading is public ---
$publicList = Get-Json "$base/problems"
Check "Question feed is readable without a token" ($publicList.success -eq $true)
Check "Feed is paginated" ($null -ne $publicList.data.items -and $null -ne $publicList.data.totalItems)

$unauthPostRejected = $false
try {
    Post-Json "$base/problems" @{ title = "Nope"; description = "x"; category = "Programming" } $null | Out-Null
} catch { $unauthPostRejected = $true }
Check "Asking requires authentication" $unauthPostRejected

# --- 2. Ask a question ---
$question = Post-Json "$base/problems" @{
    title = "Why does my loop skip an item? ($stamp)"
    description = "I iterate a list and remove items inside the loop. Some items are skipped."
    category = "Programming"
    level = "BEGINNER"
} $askerToken
$qid = $question.data.id
Check "Question created" ($qid -gt 0)
Check "New questions start OPEN" ($question.data.status -eq 'OPEN')
Check "Question records the asker" ($question.data.author.id -eq $askerId)
Check "Author email is not exposed" (-not ($question.data.author.PSObject.Properties.Name -contains 'email'))

# --- 3. It is discoverable ---
$search = Get-Json "$base/problems?query=$stamp"
Check "Full-text search finds it" (@($search.data.items | Where-Object { $_.id -eq $qid }).Count -eq 1)

$byCategory = Get-Json "$base/problems?category=Programming"
Check "Category filter finds it" (@($byCategory.data.items | Where-Object { $_.id -eq $qid }).Count -eq 1)

$badStatusRejected = $false
try { Get-Json "$base/problems?status=NONSENSE" | Out-Null } catch { $badStatusRejected = $true }
Check "Unknown status is rejected" $badStatusRejected

# --- 4. Answer rules ---
$ownAnswerRejected = $false
try {
    Post-Json "$base/problems/$qid/answers" @{ body = "Talking to myself." } $askerToken | Out-Null
} catch { $ownAnswerRejected = $true }
Check "Asker cannot answer their own question" $ownAnswerRejected

$answer = Post-Json "$base/problems/$qid/answers" @{
    body = "Do not mutate a list while iterating; build a new list instead."
} $helperToken
$answerId = $answer.data.id
Check "Another user can answer" ($answerId -gt 0)
Check "Answer starts unaccepted" ($answer.data.accepted -eq $false)

$dupRejected = $false
try {
    Post-Json "$base/problems/$qid/answers" @{ body = "Second answer." } $helperToken | Out-Null
} catch { $dupRejected = $true }
Check "One user cannot answer twice" $dupRejected

$detail = Get-Json "$base/problems/$qid"
Check "Detail shows the answer" (@($detail.data.answers).Count -eq 1)
Check "Detail counts views" ($detail.data.views -ge 1)
Check "Detail lists the answerer" ($detail.data.answers[0].author.id -eq $helperId)

# --- 5. Accepting ---
$outsiderAcceptRejected = $false
try {
    Put-Json "$base/problems/$qid/answers/$answerId/accept" $helperToken | Out-Null
} catch { $outsiderAcceptRejected = $true }
Check "Only the asker can accept" $outsiderAcceptRejected

$accepted = Put-Json "$base/problems/$qid/answers/$answerId/accept" $askerToken
Check "Accepting marks the question SOLVED" ($accepted.data.status -eq 'SOLVED')
Check "Accepted flag is set" ($accepted.data.answers[0].accepted -eq $true)

$secondAcceptRejected = $false
try {
    Put-Json "$base/problems/$qid/answers/$answerId/accept" $askerToken | Out-Null
} catch { $secondAcceptRejected = $true }
Check "A second accept is rejected" $secondAcceptRejected

# --- 6. Reputation ---
$helperProfile = Get-Json "$base/users/$helperId/profile"
Check "Answerer earns 10 reputation" ($helperProfile.data.helpfulPoints -eq 10)
Check "Answerer stats record the answer" ($helperProfile.data.answersGiven -eq 1)
Check "Answerer stats record the accept" ($helperProfile.data.acceptedAnswers -eq 1)

$askerProfile = Get-Json "$base/users/$askerId/profile"
Check "Asker stats record the question" ($askerProfile.data.questionsAsked -eq 1)
Check "Asker earns no reputation for asking" ($askerProfile.data.helpfulPoints -eq 0)

# --- 7. Status filters reflect the change ---
$solved = Get-Json "$base/problems?status=SOLVED&query=$stamp"
Check "Solved filter finds it" (@($solved.data.items | Where-Object { $_.id -eq $qid }).Count -eq 1)
Check "Solved item reports its answer count" (@($solved.data.items | Where-Object { $_.id -eq $qid })[0].answerCount -eq 1)

$open = Get-Json "$base/problems?status=OPEN&query=$stamp"
Check "Open filter no longer finds it" (@($open.data.items | Where-Object { $_.id -eq $qid }).Count -eq 0)

# --- 8. My questions is scoped and private ---
$mineUnauthRejected = $false
try {
    Invoke-WebRequest -Uri "$base/problems/mine" -UseBasicParsing -TimeoutSec 5 | Out-Null
} catch { $mineUnauthRejected = $true }
Check "My questions requires authentication" $mineUnauthRejected

$mine = Get-Json "$base/problems/mine" $askerToken
Check "My questions returns only my post" (@($mine.data | Where-Object { $_.id -eq $qid }).Count -eq 1)

# --- 9. Close / reopen on a fresh question ---
$second = Post-Json "$base/problems" @{
    title = "Close/reopen check ($stamp)"
    description = "Temporary question used to verify the close and reopen flow."
    category = "Programming"
} $askerToken
$qid2 = $second.data.id

$closed = Put-Json "$base/problems/$qid2/close" $askerToken
Check "Author can close a question" ($closed.data.status -eq 'CLOSED')

$closedAnswerRejected = $false
try {
    Post-Json "$base/problems/$qid2/answers" @{ body = "Too late." } $helperToken | Out-Null
} catch { $closedAnswerRejected = $true }
Check "Closed questions reject new answers" $closedAnswerRejected

$reopened = Put-Json "$base/problems/$qid2/reopen" $askerToken
Check "Author can reopen a closed question" ($reopened.data.status -eq 'OPEN')

$reopenedAnswer = Post-Json "$base/problems/$qid2/answers" @{ body = "Now it works." } $helperToken
Check "Reopened questions accept answers again" ($reopenedAnswer.data.id -gt 0)

# --- 10. Unknown question ---
$missingRejected = $false
try { Get-Json "$base/problems/999999999" | Out-Null } catch { $missingRejected = $true }
Check "Unknown question returns an error" $missingRejected

$passed = @($results | Where-Object { $_ }).Count
$total = @($results).Count
Write-Host "`n=== $passed / $total checks passed ===`n" -ForegroundColor $(if ($passed -eq $total) { 'Green' } else { 'Red' })
if ($passed -ne $total) { exit 1 }
