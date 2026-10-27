# Live end-to-end smoke test for the SkillSwap swap flow.
# Requires the backend running on http://localhost:9090
# Usage:  powershell -ExecutionPolicy Bypass -File .\e2e-smoke.ps1

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

Write-Host "`n=== SkillSwap end-to-end smoke test ===`n" -ForegroundColor Cyan

# 1. Health
$health = Get-Json "$base/health"
Check "Health endpoint returns UP" ($health.data.status -eq 'UP')

# 2. Register two users
$alice = Post-Json "$base/auth/register" @{
    name = "Alice E2E"; username = "alice_e2e_$stamp"; email = "alice_e2e_$stamp@test.com"
    password = "secret123"; college = "Test College"; branch = "CSE"; year = "2nd Year"; mode = "Both"
}
$bob = Post-Json "$base/auth/register" @{
    name = "Bob E2E"; username = "bob_e2e_$stamp"; email = "bob_e2e_$stamp@test.com"
    password = "secret123"; college = "Test College"; branch = "IT"; year = "3rd Year"; mode = "Both"
}
$aliceToken = $alice.data.accessToken
$bobToken = $bob.data.accessToken
$aliceId = $alice.data.user.id
$bobId = $bob.data.user.id
Check "Alice registered with a token" ([bool]$aliceToken)
Check "Bob registered with a token" ([bool]$bobToken)

# 3. Look up skills
$skills = @((Get-Json "$base/skills").data)
$python = $skills | Where-Object { $_.name -eq 'Python' } | Select-Object -First 1
$react  = $skills | Where-Object { $_.name -eq 'React' }  | Select-Object -First 1
Check "Skill catalog seeded (Python + React present)" ($null -ne $python -and $null -ne $react)

# 4. Alice teaches Python, wants React. Bob teaches React, wants Python.
Post-Json "$base/users/skills" @{ skillId = $python.id; skillType = "TEACH"; proficiency = "INTERMEDIATE" } $aliceToken | Out-Null
Post-Json "$base/users/skills" @{ skillId = $react.id;  skillType = "LEARN"; proficiency = "BEGINNER" } $aliceToken | Out-Null
Post-Json "$base/users/skills" @{ skillId = $react.id;  skillType = "TEACH"; proficiency = "ADVANCED" } $bobToken   | Out-Null
Post-Json "$base/users/skills" @{ skillId = $python.id; skillType = "LEARN"; proficiency = "BEGINNER" } $bobToken   | Out-Null

$aliceSkills = @((Get-Json "$base/users/skills" $aliceToken).data)
Check "Alice has 2 skills stored" ($aliceSkills.Count -eq 2)
Check "Alice's skills resolve correctly" (@($aliceSkills | Where-Object { $_.skillType -eq 'TEACH' }).skillName -contains 'Python')

# 5. Marketplace
$marketPython = @((Get-Json "$base/marketplace?query=Python").data)
Check "Marketplace finds Alice teaching Python" (@($marketPython | Where-Object { $_.teacherId -eq $aliceId }).Count -ge 1)

$marketReact = @((Get-Json "$base/marketplace?query=React").data)
Check "Marketplace finds Bob teaching React" (@($marketReact | Where-Object { $_.teacherId -eq $bobId }).Count -ge 1)

# 6. Mutual matching
$mutual = @((Get-Json "$base/marketplace/matches?offered=Python&wanted=React" $aliceToken).data)
Check "Mutual match finds Bob (teaches React, wants Python)" (@($mutual | Where-Object { $_.teacherId -eq $bobId }).Count -ge 1)

# 7. Propose swap
$swap = Post-Json "$base/swaps" @{
    receiverId = $bobId; offeredSkillId = $python.id; requestedSkillId = $react.id
    message = "Hi Bob, want to swap Python for React?"
} $aliceToken
$swapId = $swap.data.id
Check "Swap proposed with PENDING status" ($swap.data.status -eq 'PENDING')
Check "Swap response does not leak Alice's email" (-not ($swap.data.sender.PSObject.Properties.Name -contains 'email'))

# 8. Duplicate proposal is rejected
$dupRejected = $false
try {
    Post-Json "$base/swaps" @{
        receiverId = $bobId; offeredSkillId = $python.id; requestedSkillId = $react.id
    } $aliceToken | Out-Null
} catch { $dupRejected = $true }
Check "Duplicate proposal rejected" $dupRejected

# 9. Sender cannot accept own swap
$selfAcceptRejected = $false
try { Put-Json "$base/swaps/$swapId/accept" $aliceToken | Out-Null } catch { $selfAcceptRejected = $true }
Check "Sender cannot accept their own swap" $selfAcceptRejected

# 10. Bob sees it as incoming
$incoming = @((Get-Json "$base/swaps/incoming" $bobToken).data)
Check "Bob sees the incoming swap" (@($incoming | Where-Object { $_.id -eq $swapId }).Count -ge 1)

# 11. Bob accepts -> meeting link
$accepted = Put-Json "$base/swaps/$swapId/accept" $bobToken
Check "Swap accepted" ($accepted.data.status -eq 'ACCEPTED')
Check "Meeting link generated" ($accepted.data.meetingLink -like 'https://meet.jit.si/*')

# 12. Complete
$completed = Put-Json "$base/swaps/$swapId/complete" $aliceToken
Check "Swap completed" ($completed.data.status -eq 'COMPLETED')

# 13. Public profile exposes no email
$profile = Get-Json "$base/users/$bobId/profile"
Check "Public profile returns Bob" ($profile.data.user.username -eq "bob_e2e_$stamp")
Check "Public profile hides email" (-not ($profile.data.user.PSObject.Properties.Name -contains 'email'))
Check "Public profile includes Bob's skills" (@($profile.data.skills).Count -eq 2)

# 14. Unauthenticated access returns JSON 401
$unauthJson = $false
try {
    Invoke-WebRequest -Uri "$base/users/me" -UseBasicParsing -TimeoutSec 5 | Out-Null
} catch {
    $body = $_.ErrorDetails.Message
    if ($body -and $body -like '*"success":false*') { $unauthJson = $true }
}
Check "Unauthenticated request returns JSON 401" $unauthJson

$passed = @($results | Where-Object { $_ }).Count
$total = @($results).Count
Write-Host "`n=== $passed / $total checks passed ===`n" -ForegroundColor $(if ($passed -eq $total) { 'Green' } else { 'Red' })
if ($passed -ne $total) { exit 1 }
