# Family Sync Workflow — Step Description

| Field | Value |
|---|---|
| Diagram | `main work flow-4. Family Sync Workflow.drawio` / `.drawio.png` |
| Folder | `03_Design/Workflow/` |
| Diagram type | Activity / Swimlane workflow |
| Swimlanes | Mother (Group Owner), System, Family Member |
| Purpose | Describes how a mother creates a care group, brings family members into it, decides what information each member may see, and how members then view the shared information. |

## Description Table

| # | Step Name | Detail Description | Role | Note |
|---|---|---|---|---|
| 1 | Create care group | The mother creates a care group to begin sharing care information with her family. | Mother | The care group is the container that holds members, shared information and sharing permissions. A mother may keep several active care groups, each with a distinct name. |
| 2 | Save group, auto-add Mother as OWNER | The system creates the care group and records the mother as its owner. The group is linked to her active care journey so that shared schedules and checklists can later be presented to members. | System | The mother is the only owner of the group. Ownership determines who may invite members and manage sharing permissions. |
| 3 | Invite family member or share group code | The mother either sends an invitation directly to a family member's phone number or email address, or shares the care group code so the family member can request to join. | Mother | Only the group owner may invite members or approve new members. |
| 4 | Invite channel? | The workflow branches according to how the mother brings the family member in: a direct invitation, or a shared care group code. | System | Both branches lead to the same membership result, but they differ in who starts the request and who confirms it. |
| 5A | Create an invitation and send a notification to the phone number/email | The system matches the phone number or email address to an existing CareBridge account, records a pending invitation for that person and notifies them. | System | The invited person must already have a CareBridge account. An invitation stays valid for a limited period, and a group may hold only a limited number of pending invitations at a time. |
| 6A | Open pending invitation, choose family relationship role and accept | The family member opens the pending invitation, selects their relationship to the mother and accepts it. | Family Member | The family member may also decline. An invitation can be accepted only once, and cannot be accepted after it expires. |
| 5B | Enter shared group code and choose family relationship role | The family member enters the care group code shared by the mother and selects their relationship to the mother. | Family Member | The code works only for an active care group. Someone who is already a member, or already waiting for approval, cannot submit it again. |
| 6B | Create join request and notify Mother | The system records the submission as a join request awaiting the mother's decision and notifies her. | System | A join request gives no access to the group or its information until the mother approves it. |
| 7 | Mother approves request? | The mother reviews the pending join requests for her care group and approves or rejects each one. | Mother | This approval step applies only to the shared-code branch. On the direct-invitation branch, the invited person confirms instead. |
| 7B | Join request rejected | The join request is rejected and the family member does not join the care group. | Family Member | The rejection is not permanent. The same person may submit the care group code again later. |
| 8 | Activate member and notify Mother | The system activates the family member in the care group, records the date they joined and notifies the mother. | System | At this point the member belongs to the group but still cannot see any shared information. |
| 9 | Grant per-member sharing permissions | The mother chooses, for each member individually, which information may be shared: calendar, care logs, alerts, health records, checklists and quick notes such as weight, hydration, mood screening, fetal movement, blood pressure and blood glucose. | Mother | Permissions are granted only after the member has joined; they cannot be assigned in advance. Each member may be given a different level of access. |
| 10 | Save member permission | The system saves the sharing permissions chosen for the member and applies them to everything that member can see from that point on. | System | The mother may change or withdraw any permission at any time, and the change takes effect immediately. |
| 11 | Open family dashboard or shared data screen | The family member opens the family dashboard or the shared information screen for a care group they belong to. | Family Member | Only an active member of the care group may open these screens. |
| 12 | View shared calendar, alerts, tasks and health metrics | The family member sees the shared calendar, alerts, care tasks and health information that the mother has permitted for them. | Family Member | Information the member has not been given permission for is not displayed. The mother, as owner, sees all information in her own group. |
| Boundary | Workflow boundary | Family Sync shares only the information the mother has agreed to share. Every permission is granted by the mother after a member joins, and may be withdrawn at any time. Membership by itself gives no access to information. | System / Mother | This boundary applies to all activities in the workflow. |

## Role Legend

| Role | Meaning |
|---|---|
| Mother | The owner of the care group. The only role that may invite members, approve join requests, and grant or withdraw sharing permissions. |
| System | The CareBridge platform, which records the group and its members, manages invitations and join requests, applies sharing permissions and sends notifications. |
| Family Member | An invited or approved relative. Sees only the information the mother has explicitly shared with them. |
