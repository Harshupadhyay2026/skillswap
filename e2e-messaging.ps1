# Live end-to-end test for Phase 3.7 / 3.9 / Phase 4:
#   avatar upload, contributor leaderboard, messaging and notifications.
# Requires the backend running on http://localhost:9090
# Usage:  powershell -ExecutionPolicy Bypass -File .\e2e-messaging.ps1

$ErrorActionPreference = 'Stop'
$base = 'http://localhost:9090/api'
$origin = 'http://localhost:9090'
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

function Delete-Json($url, $token) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    Invoke-RestMethod -Uri $url -Method Delete -Headers $headers
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

Write-Host "`n=== SkillSwap avatars / leaderboard / messaging / notifications e2e ===`n" -ForegroundColor Cyan

$health = Get-Json "$base/health"
Check "Health endpoint returns UP" ($health.data.status -eq 'UP')

# ── Setup ────────────────────────────────────────────────────────────────────
$a = Post-Json "$base/auth/register" @{
    name = "Alice Chat"; username = "alice_chat_e2e_$stamp"; email = "alice_chat_e2e_$stamp@test.com"
    password = "secret123"; college = "Test College"; branch = "CSE"; year = "2nd Year"; mode = "Both"
}
$b = Post-Json "$base/auth/register" @{
    name = "Bob Chat"; username = "bob_chat_e2e_$stamp"; email = "bob_chat_e2e_$stamp@test.com"
    password = "secret123"; college = "Test College"; branch = "IT"; year = "3rd Year"; mode = "Both"
}
$c = Post-Json "$base/auth/register" @{
    name = "Carol Outsider"; username = "carol_chat_e2e_$stamp"; email = "carol_chat_e2e_$stamp@test.com"
    password = "secret123"; college = "Test College"; branch = "ECE"; year = "1st Year"; mode = "Both"
}
$aToken = $a.data.accessToken; $aId = $a.data.user.id
$bToken = $b.data.accessToken; $bId = $b.data.user.id
$cToken = $c.data.accessToken; $cId = $c.data.user.id

Check "Three users registered" ($aId -gt 0 -and $bId -gt 0 -and $cId -gt 0)

# ── Phase 3.7 — Avatar upload ───────────────────────────────────────────────
$pngPath = Join-Path $env:TEMP "skillswap-avatar-$stamp.png"
$badPath = Join-Path $env:TEMP "skillswap-avatar-$stamp.txt"
$pngBase64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
[IO.File]::WriteAllBytes($pngPath, [Convert]::FromBase64String($pngBase64))
Set-Content -Path $badPath -Value 'this is not an image' -NoNewline

$avatarRaw = curl.exe -s -X POST "$base/users/me/avatar" -H "Authorization: Bearer $aToken" -F "file=@$pngPath;type=image/png"
$avatar = $avatarRaw | ConvertFrom-Json
Check "Avatar upload succeeds" ($avatar.success -eq $true)
Check "Avatar URL is under /api/files/avatars" ($avatar.data.avatarUrl -like '/api/files/avatars/*')

$avatarFetch = Invoke-WebRequest -Uri "$origin$($avatar.data.avatarUrl)" -UseBasicParsing
Check "Uploaded avatar is served publicly" ($avatarFetch.StatusCode -eq 200)
Check "Avatar is served as an image" ($avatarFetch.Headers['Content-Type'] -like 'image/*')
Check "Avatar response is non-empty" ($avatarFetch.RawContentLength -gt 0)

$badStatus = curl.exe -s -o NUL -w "%{http_code}" -X POST "$base/users/me/avatar" -H "Authorization: Bearer $aToken" -F "file=@$badPath;type=image/png"
Check "Non-image upload is rejected" ($badStatus -eq '400')

$anonAvatarStatus = curl.exe -s -o NUL -w "%{http_code}" -X POST "$base/users/me/avatar" -F "file=@$pngPath;type=image/png"
Check "Avatar upload requires login" ($anonAvatarStatus -eq '401')

# ── Phase 4 — Messaging ─────────────────────────────────────────────────────
$conversation = Post-Json "$base/conversations" @{ userId = $bId } $aToken
$cid = $conversation.data.id
Check "Conversation created" ($cid -gt 0)
Check "Counterpart is the other user" ($conversation.data.counterpart.id -eq $bId)
Check "Counterpart email is not exposed" (-not ($conversation.data.counterpart.PSObject.Properties.Name -contains 'email'))

$again = Post-Json "$base/conversations" @{ userId = $bId } $aToken
Check "Starting the same conversation is idempotent" ($again.data.id -eq $cid)
$fromOtherSide = Post-Json "$base/conversations" @{ userId = $aId } $bToken
Check "Both sides resolve to one thread" ($fromOtherSide.data.id -eq $cid)

$selfRejected = $false
try { Post-Json "$base/conversations" @{ userId = $aId } $aToken | Out-Null } catch { $selfRejected = $true }
Check "Cannot message yourself" $selfRejected

$sent = Post-Json "$base/conversations/$cid/messages" @{ body = "Hi Bob, ready to swap? ($stamp)" } $aToken
Check "Message sent" ($sent.data.id -gt 0)
Check "Message records the sender" ($sent.data.sender.id -eq $aId)

$bUnread = Get-Json "$base/conversations/unread-count" $bToken
Check "Recipient has one unread message" ($bUnread.data.count -eq 1)

$aUnread = Get-Json "$base/conversations/unread-count" $aToken
Check "Sender has no unread message from themselves" ($aUnread.data.count -eq 0)

$bInbox = Get-Json "$base/conversations" $bToken
Check "Recipient sees the conversation" (@($bInbox.data | Where-Object { $_.id -eq $cid }).Count -eq 1)
$bRow = @($bInbox.data | Where-Object { $_.id -eq $cid })[0]
Check "Inbox shows the counterpart" ($bRow.counterpart.id -eq $aId)
Check "Inbox shows a preview" ($bRow.lastMessage -like 'Hi Bob*')

$readResult = Put-Json "$base/conversations/$cid/read" $bToken
Check "Mark read updates the message" ($readResult.data.updated -eq 1)
$bUnreadAfter = Get-Json "$base/conversations/unread-count" $bToken
Check "Mark read clears the badge" ($bUnreadAfter.data.count -eq 0)

Post-Json "$base/conversations/$cid/messages" @{ body = "Yes! When works for you?" } $bToken | Out-Null
$thread = Get-Json "$base/conversations/$cid/messages" $aToken
Check "Thread contains both messages" (@($thread.data).Count -eq 2)
Check "Messages are oldest-first" ($thread.data[0].body -like 'Hi Bob*')
Check "Second message is the reply" ($thread.data[1].body -like 'Yes!*')

$blankRejected = $false
try { Post-Json "$base/conversations/$cid/messages" @{ body = "   " } $aToken | Out-Null } catch { $blankRejected = $true }
Check "Blank messages are rejected" $blankRejected

$outsiderDenied = $false
try { Get-Json "$base/conversations/$cid/messages" $cToken | Out-Null } catch { $outsiderDenied = ($_.Exception.Response.StatusCode.value__ -eq 404) }
Check "Outsiders cannot read a thread (404)" $outsiderDenied

$outsiderSendDenied = $false
try { Post-Json "$base/conversations/$cid/messages" @{ body = "Sneaking in" } $cToken | Out-Null } catch { $outsiderSendDenied = $true }
Check "Outsiders cannot write to a thread" $outsiderSendDenied

# ── Phase 4 — Notifications (swaps, learning, Q&A) ──────────────────────────
$skills = @((Get-Json "$base/skills").data)
$python = $skills | Where-Object { $_.name -eq 'Python' } | Select-Object -First 1
$react = $skills | Where-Object { $_.name -eq 'React' } | Select-Object -First 1

Post-Json "$base/users/skills" @{ skillId = $python.id; skillType = 'TEACH'; proficiency = 'INTERMEDIATE' } $aToken | Out-Null
Post-Json "$base/users/skills" @{ skillId = $react.id; skillType = 'TEACH'; proficiency = 'INTERMEDIATE' } $bToken | Out-Null

$swap = Post-Json "$base/swaps" @{
    receiverId = $bId; offeredSkillId = $python.id; requestedSkillId = $react.id; message = "Let's swap"
} $aToken

$bNotifications = Get-Json "$base/notifications" $bToken
$proposalNotif = @($bNotifications.data.items | Where-Object { $_.type -eq 'SWAP_PROPOSED' })[0]
Check "Swap proposal notifies the receiver" ($null -ne $proposalNotif)
Check "Proposal notification links to /requests" ($proposalNotif.link -eq '/requests')

$bBeforeAccept = (Get-Json "$base/notifications/unread-count" $bToken).data.count
Put-Json "$base/swaps/$($swap.data.id)/accept" $bToken | Out-Null
$aAfterAccept = Get-Json "$base/notifications" $aToken
Check "Acceptance notifies the sender" (@($aAfterAccept.data.items | Where-Object { $_.type -eq 'SWAP_ACCEPTED' }).Count -ge 1)
$bAfterAccept = (Get-Json "$base/notifications/unread-count" $bToken).data.count
Check "Actor is not notified of their own action" ($bAfterAccept -eq $bBeforeAccept)

$learning = Post-Json "$base/learning" @{ teacherId = $aId; skillId = $python.id; message = "Teach me Python" } $bToken
$aLearningNotifs = Get-Json "$base/notifications" $aToken
Check "Learning request notifies the teacher" (@($aLearningNotifs.data.items | Where-Object { $_.type -eq 'LEARNING_REQUESTED' }).Count -ge 1)

Put-Json "$base/learning/$($learning.data.id)/accept" $aToken | Out-Null
$bLearningNotifs = Get-Json "$base/notifications" $bToken
Check "Learning acceptance notifies the learner" (@($bLearningNotifs.data.items | Where-Object { $_.type -eq 'LEARNING_ACCEPTED' }).Count -ge 1)

# Q&A -> accepted answer -> reputation + notification + leaderboard
$question = Post-Json "$base/problems" @{
    title = "Loop question ($stamp)"; description = "Why does my loop skip an item?"; category = "Programming"; level = "BEGINNER"
} $aToken
$answer = Post-Json "$base/problems/$($question.data.id)/answers" @{ body = "Iterate over a copy instead." } $bToken
Put-Json "$base/problems/$($question.data.id)/answers/$($answer.data.id)/accept" $aToken | Out-Null

$bQaNotifs = Get-Json "$base/notifications" $bToken
$answerNotif = @($bQaNotifs.data.items | Where-Object { $_.type -eq 'ANSWER_ACCEPTED' })[0]
Check "Accepted answer notifies the answerer" ($null -ne $answerNotif)
Check "Answer notification links to the question" ($answerNotif.link -eq "/qa/$($question.data.id)")

# Mark one notification read, then all.
$notifId = $proposalNotif.id
$bUnreadBeforeOne = (Get-Json "$base/notifications/unread-count" $bToken).data.count
Put-Json "$base/notifications/$notifId/read" $bToken | Out-Null
$bAfterOne = (Get-Json "$base/notifications/unread-count" $bToken).data.count
Check "Individual notification can be marked read" ($bAfterOne -eq ($bUnreadBeforeOne - 1))

$outsiderReadDenied = $false
try { Put-Json "$base/notifications/$notifId/read" $cToken | Out-Null } catch { $outsiderReadDenied = ($_.Exception.Response.StatusCode.value__ -eq 404) }
Check "You cannot read someone else's notification (404)" $outsiderReadDenied

Put-Json "$base/notifications/read-all" $bToken | Out-Null
$bFinalUnread = (Get-Json "$base/notifications/unread-count" $bToken).data.count
Check "Mark all read clears the badge" ($bFinalUnread -eq 0)

$anonNotifDenied = $false
try { Get-Json "$base/notifications" $null | Out-Null } catch { $anonNotifDenied = $true }
Check "Notifications require login" $anonNotifDenied

# ── Phase 3.9 — Leaderboard ─────────────────────────────────────────────────
$board = Get-Json "$base/community/top-contributors"
Check "Leaderboard is public" ($board.success -eq $true)
$bRow = @($board.data | Where-Object { $_.userId -eq $bId })[0]
Check "Answerer appears on the leaderboard" ($null -ne $bRow)
Check "Answerer earned 10 reputation" ($bRow.helpfulPoints -ge 10)
Check "Accepted answer count is tracked" ($bRow.acceptedAnswers -ge 1)
Check "Leaderboard rows carry a rank" ($bRow.rank -ge 1)

$limited = Get-Json "$base/community/top-contributors?limit=1"
Check "Leaderboard limit is respected" (@($limited.data).Count -le 1)

# ── Avatar removal ──────────────────────────────────────────────────────────
$removed = Delete-Json "$base/users/me/avatar" $aToken
Check "Avatar can be removed" ($null -eq $removed.data.avatarUrl)

Remove-Item $pngPath -ErrorAction SilentlyContinue
Remove-Item $badPath -ErrorAction SilentlyContinue

# ── Summary ─────────────────────────────────────────────────────────────────
$passed = @($results | Where-Object { $_ }).Count
$total = $results.Count
Write-Host "`n=== Result: $passed/$total checks passed ===`n" -ForegroundColor $(if ($passed -eq $total) { 'Green' } else { 'Red' })
if ($passed -ne $total) { exit 1 }
