<script lang="ts">
    import { onMount } from 'svelte';
    import { base } from '$app/paths';
    import { page } from '$app/stores';
    import ConfirmationModal from '$lib/components/ConfirmationModal.svelte';
    import { fetchApi, fetchJson, ApiError } from '$lib/api';
    import { auth, getAvatarUrl } from '$lib/stores/auth.svelte';
    import { toast } from '$lib/stores/toast.svelte';
    import type { GroupResponse, GroupMemberResponse, ScheduleResponse, PartyResponse, AvailabilityResponse, CharacterResponse } from '$lib/types/api';

    let groupId = $derived($page.url.searchParams.get('id') ?? '');
    let group = $state<GroupResponse | null>(null);
    let members = $state<GroupMemberResponse[]>([]);
    let schedules = $state<ScheduleResponse[]>([]);
    let parties = $state<PartyResponse[]>([]);
    let rawAvailability = $state<AvailabilityResponse[]>([]);
    let heatmapData = $state<number[][] | null>(null);
    let myCharacters = $state<CharacterResponse[]>([]);

    let isLoading = $state(true);
    let error = $state('');

    let openHeatmapId = $state<string | null>(null);
    let expandedPartyId = $state<string | null>(null);
    let loadingPartyId = $state<string | null>(null);
    let selectedCell = $state<{ d: number; t: number; members: string[] } | null>(null);

    // Character form state
    let showCharacterForm = $state(false);
    let editingCharacter = $state<CharacterResponse | null>(null);
    let charName = $state('');
    let charClass = $state('');
    let charIlvl = $state('');
    let charFormLoading = $state(false);

    // Join party modal
    let joinModalParty = $state<{ party: PartyResponse; scheduleId: string } | null>(null);
    let joinModalCharId = $state<string>('');
    let joinModalLoading = $state(false);
    let joinModalDialog: HTMLDialogElement;

    // Rename schedule modal
    let renameModalSchedule = $state<ScheduleResponse | null>(null);
    let renameTitle = $state('');
    let renameLoading = $state(false);
    let renameModalDialog: HTMLDialogElement;

    // Edit party modal
    let editModalParty = $state<{ party: PartyResponse; scheduleId: string } | null>(null);
    let editTitle = $state('');
    let editRaidType = $state('');
    let editLoading = $state(false);
    let editModalDialog: HTMLDialogElement;

    // History modal
    let archivedSchedules = $state<ScheduleResponse[]>([]);
    let historyLoading = $state(false);
    let historyModalDialog: HTMLDialogElement;

    let isManager = $derived(members.find(m => m.id === auth.user?.id)?.role === 'MANAGER');
    let isAuditor = $derived(members.find(m => m.id === auth.user?.id)?.role === 'AUDITOR');
    let isAdmin = $derived(isManager || isAuditor);

    let deleteScheduleModal: ReturnType<typeof ConfirmationModal>;
    let scheduleToDelete = $state<string | null>(null);

    let archiveScheduleModal: ReturnType<typeof ConfirmationModal>;
    let scheduleToArchive = $state<string | null>(null);

    let deletePartyModal: ReturnType<typeof ConfirmationModal>;
    let partyToDelete = $state<PartyResponse | null>(null);

    let kickMemberModal: ReturnType<typeof ConfirmationModal>;
    let memberToKick = $state<GroupMemberResponse | null>(null);

    async function fetchData() {
        try {
            const [groupData, memberData, scheduleData, characterData] = await Promise.all([
                fetchJson<GroupResponse>(`/api/groups/detail?groupId=${groupId}`),
                fetchJson<GroupMemberResponse[]>(`/api/members?groupId=${groupId}`),
                fetchJson<ScheduleResponse[]>(`/api/schedules/list?groupId=${groupId}`),
                fetchJson<CharacterResponse[]>(`/api/characters/me?groupId=${groupId}`)
            ]);
            group = groupData;
            members = memberData;
            schedules = scheduleData;
            myCharacters = characterData;
            if (schedules.length > 0) {
                await Promise.all(schedules.map(s => fetchParties(s.id)));
            }
        } catch (err) {
            if (!(err instanceof ApiError && err.status === 401)) {
                error = 'Failed to load group details.';
            }
        } finally {
            isLoading = false;
        }
    }

    async function fetchParties(scheduleId: string) {
        try {
            const data = await fetchJson<PartyResponse[]>(`/api/schedules/parties?scheduleId=${scheduleId}`);
            parties = [...parties.filter(p => p.scheduleId !== scheduleId), ...data.map(p => ({ ...p, scheduleId }))];
        } catch {
            // non-fatal; parties section will show empty
        }
    }

    async function fetchHeatmap(scheduleId: string, startDate: string) {
        try {
            const data = await fetchJson<AvailabilityResponse[]>(`/api/schedules/availability?scheduleId=${scheduleId}`);
            rawAvailability = data;

            const grid = Array.from({ length: 8 }, () => Array(24).fill(0));
            const baseDate = new Date(startDate);
            baseDate.setHours(0, 0, 0, 0);

            for (const userAvail of data) {
                for (const block of userAvail.blocks) {
                    const diffHours = Math.floor((new Date(block.start).getTime() - baseDate.getTime()) / 3_600_000);
                    const d = Math.floor(diffHours / 24);
                    const t = diffHours % 24;
                    if (d >= 0 && d < 8 && t >= 0 && t < 24) grid[d][t]++;
                }
            }
            heatmapData = grid;
        } catch {
            // non-fatal
        }
    }

    async function toggleHeatmap(id: string, startDate: string) {
        if (openHeatmapId !== id) await fetchHeatmap(id, startDate);
        openHeatmapId = openHeatmapId === id ? null : id;
        selectedCell = null;
    }

    function openRenameModal(schedule: ScheduleResponse) {
        renameModalSchedule = schedule;
        renameTitle = schedule.title;
        renameModalDialog.showModal();
    }

    async function submitRename() {
        if (!renameModalSchedule || !renameTitle.trim()) return;
        renameLoading = true;
        const s = renameModalSchedule;
        try {
            await fetchApi(`/api/schedules/update`, { method: 'POST', body: JSON.stringify({ scheduleId: s.id, title: renameTitle.trim(), startTime: s.start, endTime: s.end }) });
            await fetchData();
            toast.success('Schedule renamed.');
        } catch {
            toast.error('Failed to rename schedule.');
        }
        renameModalDialog.close();
        renameModalSchedule = null;
        renameLoading = false;
    }

    function requestDeleteSchedule(scheduleId: string) {
        scheduleToDelete = scheduleId;
        deleteScheduleModal.show();
    }

    async function confirmDeleteSchedule() {
        if (!scheduleToDelete) return;
        try {
            await fetchApi(`/api/schedules/delete`, { method: 'POST', body: JSON.stringify({ scheduleId: scheduleToDelete }) });
            await fetchData();
            toast.success('Schedule deleted.');
        } catch {
            toast.error('Failed to delete schedule.');
        } finally {
            scheduleToDelete = null;
        }
    }

    function requestArchiveSchedule(scheduleId: string) {
        scheduleToArchive = scheduleId;
        archiveScheduleModal.show();
    }

    async function confirmArchiveSchedule() {
        if (!scheduleToArchive) return;
        try {
            await fetchApi(`/api/schedules/archive`, { method: 'POST', body: JSON.stringify({ scheduleId: scheduleToArchive }) });
            await fetchData();
            toast.success('Schedule archived.');
        } catch {
            toast.error('Failed to archive schedule.');
        } finally {
            scheduleToArchive = null;
        }
    }

    async function unarchiveSchedule(scheduleId: string) {
        try {
            await fetchApi(`/api/schedules/unarchive`, { method: 'POST', body: JSON.stringify({ scheduleId }) });
            archivedSchedules = archivedSchedules.filter(s => s.id !== scheduleId);
            await fetchData();
            toast.success('Schedule restored to active list.');
        } catch {
            toast.error('Failed to unarchive schedule.');
        }
    }

    async function openHistoryModal() {
        historyLoading = true;
        historyModalDialog.showModal();
        try {
            archivedSchedules = await fetchJson<ScheduleResponse[]>(`/api/schedules/archived?groupId=${groupId}`);
        } catch {
            toast.error('Failed to load schedule history.');
        } finally {
            historyLoading = false;
        }
    }

    async function toggleParty(id: string, scheduleId: string) {
        if (expandedPartyId !== id) {
            loadingPartyId = id;
            await fetchParties(scheduleId);
            loadingPartyId = null;
        }
        expandedPartyId = expandedPartyId === id ? null : id;
    }

    function requestDeleteParty(party: PartyResponse) {
        partyToDelete = party;
        deletePartyModal.show();
    }

    async function confirmDeleteParty() {
        if (!partyToDelete) return;
        try {
            await fetchApi(`/api/parties/delete`, { method: 'POST', body: JSON.stringify({ partyId: partyToDelete.id }) });
            await fetchParties(partyToDelete.scheduleId);
            toast.success('Party deleted.');
        } catch {
            toast.error('Failed to delete party.');
        } finally {
            partyToDelete = null;
        }
    }

    async function togglePartyStatus(party: PartyResponse) {
        try {
            const completed = party.status !== 'Done';
            await fetchApi(`/api/parties/complete`, { method: 'POST', body: JSON.stringify({ partyId: party.id, completed }) });
            await fetchParties(party.scheduleId);
            toast.success(completed ? 'Party marked as Done.' : 'Party marked as Planned.');
        } catch {
            toast.error('Failed to update party status.');
        }
    }

    function openJoinModal(party: PartyResponse, scheduleId: string) {
        if (myCharacters.length === 0) {
            toast.error('Register a character in the sidebar before joining a party.');
            return;
        }
        joinModalParty = { party, scheduleId };
        joinModalCharId = myCharacters[0].id;
        joinModalDialog.showModal();
    }

    async function confirmJoin() {
        if (!joinModalParty || !joinModalCharId) return;
        joinModalLoading = true;
        const { party, scheduleId } = joinModalParty;
        const charId = joinModalCharId;
        await joinParty(party.id, charId, scheduleId);
        joinModalDialog.close();
        joinModalParty = null;
        joinModalLoading = false;
    }

    function openEditModal(party: PartyResponse, scheduleId: string) {
        editModalParty = { party, scheduleId };
        editTitle = party.title;
        editRaidType = party.raidType ?? '';
        editModalDialog.showModal();
    }

    async function submitEdit() {
        if (!editModalParty || !editTitle.trim()) return;
        editLoading = true;
        const { party, scheduleId } = editModalParty;
        try {
            await fetchApi('/api/parties/update', {
                method: 'POST',
                body: JSON.stringify({ partyId: party.id, title: editTitle.trim(), raidType: editRaidType.trim() || party.raidType })
            });
            await fetchParties(scheduleId);
            toast.success('Party updated.');
        } catch {
            toast.error('Failed to update party.');
        }
        editModalDialog.close();
        editModalParty = null;
        editLoading = false;
    }

    async function joinParty(partyId: string, characterId: string, scheduleId: string) {
        try {
            await fetchApi(`/api/parties/join`, { method: 'POST', body: JSON.stringify({ partyId, characterId }) });
            await fetchParties(scheduleId);
            toast.success('Joined the party!');
        } catch {
            await fetchParties(scheduleId);
            toast.error('Failed to join. Syncing latest data...');
        }
    }

    async function leaveParty(partyId: string, characterId: string, scheduleId: string) {
        try {
            await fetchApi(`/api/parties/leave`, { method: 'POST', body: JSON.stringify({ partyId, characterId }) });
            await fetchParties(scheduleId);
            toast.success('Left the party.');
        } catch {
            await fetchParties(scheduleId);
            toast.error('Failed to leave. Syncing latest data...');
        }
    }

    function requestKickMember(member: GroupMemberResponse) {
        memberToKick = member;
        kickMemberModal.show();
    }

    async function confirmKickMember() {
        if (!memberToKick) return;
        try {
            await fetchApi(`/api/members/remove`, { method: 'POST', body: JSON.stringify({ groupId, targetUserId: memberToKick.id }) });
            await fetchData();
            toast.success(`${memberToKick.username} has been removed from the group.`);
        } catch {
            toast.error('Failed to remove member.');
        } finally {
            memberToKick = null;
        }
    }

    async function toggleAuditorRole(member: GroupMemberResponse) {
        const newRole = member.role === 'AUDITOR' ? 'MEMBER' : 'AUDITOR';
        try {
            await fetchApi(`/api/members/role`, { method: 'POST', body: JSON.stringify({ groupId, targetUserId: member.id, role: newRole }) });
            await fetchData();
            toast.success(`${member.username} is now ${newRole}.`);
        } catch {
            toast.error('Failed to update role.');
        }
    }

    function notifyMember(member: GroupMemberResponse) {
        toast.info(`Notification feature for ${member.username} is planned.`);
    }

    // Character management
    function openCharacterForm(char: CharacterResponse | null = null) {
        editingCharacter = char;
        charName = char?.name ?? '';
        charClass = char?.characterClass ?? '';
        charIlvl = char?.itemLevel?.toString() ?? '';
        showCharacterForm = true;
    }

    function cancelCharacterForm() {
        showCharacterForm = false;
        editingCharacter = null;
    }

    async function saveCharacter() {
        if (!charName.trim()) return;
        charFormLoading = true;
        try {
            if (editingCharacter) {
                await fetchApi('/api/characters/update', {
                    method: 'POST',
                    body: JSON.stringify({ groupId, characterId: editingCharacter.id, name: charName.trim(), characterClass: charClass.trim() || null, itemLevel: charIlvl ? parseFloat(charIlvl) : null })
                });
                toast.success('Character updated.');
            } else {
                await fetchApi('/api/characters/create', {
                    method: 'POST',
                    body: JSON.stringify({ groupId, name: charName.trim(), characterClass: charClass.trim() || null, itemLevel: charIlvl ? parseFloat(charIlvl) : null })
                });
                toast.success('Character registered.');
            }
            myCharacters = await fetchJson<CharacterResponse[]>(`/api/characters/me?groupId=${groupId}`);
            cancelCharacterForm();
        } catch {
            toast.error('Failed to save character.');
        } finally {
            charFormLoading = false;
        }
    }

    async function deleteCharacter(char: CharacterResponse) {
        try {
            await fetchApi('/api/characters/delete', { method: 'POST', body: JSON.stringify({ groupId, characterId: char.id }) });
            myCharacters = myCharacters.filter(c => c.id !== char.id);
            toast.success('Character deleted.');
        } catch {
            toast.error('Failed to delete character.');
        }
    }

    // Heatmap helpers
    function formatDateOffset(dateString: string, offsetDays: number) {
        const d = new Date(dateString);
        d.setDate(d.getDate() + offsetDays);
        const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
        return `${String(d.getDate()).padStart(2, '0')} (${weekdays[d.getDay()]})`;
    }

    function getFullDate(dateString: string, offsetDays: number) {
        const d = new Date(dateString);
        d.setDate(d.getDate() + offsetDays);
        return d.toLocaleDateString();
    }

    function getHeatmapClass(count: number) {
        if (!count) return 'bg-base-100';
        const ratio = count / Math.max(members.length, 1);
        if (ratio >= 0.8) return 'bg-success text-success-content';
        if (ratio >= 0.5) return 'bg-primary text-primary-content';
        if (ratio >= 0.2) return 'bg-info text-info-content';
        return 'bg-warning text-warning-content';
    }

    function handleCellClick(d: number, t: number, scheduleStart: string) {
        const baseDate = new Date(scheduleStart);
        baseDate.setHours(0, 0, 0, 0);
        const targetTime = baseDate.getTime() + (d * 24 + t) * 3_600_000;

        const availMembers = rawAvailability
            .filter(ua => ua.blocks.some(b => new Date(b.start).getTime() === targetTime))
            .map(ua => members.find(m => m.id === ua.userId)?.username ?? 'Unknown');

        selectedCell = { d, t, members: availMembers };
    }

    onMount(fetchData);
</script>

<svelte:head>
    <title>{group ? group.name : 'Loading...'} - Ark Resolver</title>
</svelte:head>

<main class="flex-1 p-4 md:p-8">
    <div class="container mx-auto max-w-6xl">

        <!-- Page header -->
        <div class="flex items-center gap-4 mb-8">
            <a href="{base}/dashboard" class="btn btn-ghost btn-circle" aria-label="Back to Dashboard">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
                </svg>
            </a>
            <h1 class="text-3xl font-bold text-ubuntu">Group Details</h1>
        </div>

        {#if isLoading}
            <div class="flex justify-center items-center h-64">
                <span class="loading loading-spinner loading-lg text-primary"></span>
            </div>

        {:else if error}
            <div class="alert alert-error shadow-lg">
                <span>{error}</span>
                <a href="{base}/dashboard" class="btn btn-sm btn-ghost">Return to Dashboard</a>
            </div>

        {:else if group}
            <!-- Group hero card -->
            <div class="card bg-base-100 shadow-xl mb-8 border-l-4 border-primary">
                <div class="card-body flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                    <div class="min-w-0">
                        <h2 class="text-4xl font-bold text-ubuntu mb-1">{group.name}</h2>
                        {#if group.description}
                            <p class="opacity-70 font-neo">{group.description}</p>
                        {/if}
                    </div>
                    <div class="flex gap-2 shrink-0">
                        {#if isAdmin}
                            <a href="{base}/groups/invites?id={groupId}" class="btn btn-primary">Invite Members</a>
                        {/if}
                        <a href="{base}/groups/settings?id={groupId}" class="btn btn-outline font-bold">Settings</a>
                    </div>
                </div>
            </div>

            <!-- Main content grid -->
            <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

                <!-- ── Schedules column ────────────────────────────────── -->
                <div class="lg:col-span-2 space-y-6">
                    <div class="flex items-center justify-between">
                        <h3 class="text-2xl font-bold text-ubuntu">Schedules</h3>
                        <div class="flex gap-2">
                            <button class="btn btn-sm btn-ghost" onclick={openHistoryModal}>History</button>
                            {#if isAdmin}
                                <a href="{base}/groups/schedules/create?id={groupId}" class="btn btn-sm btn-secondary">New Schedule</a>
                            {/if}
                        </div>
                    </div>

                    {#if schedules.length === 0}
                        <div class="card bg-base-100 shadow-md">
                            <div class="card-body text-center opacity-60 py-12">No schedules have been created yet.</div>
                        </div>
                    {:else}
                        {#each schedules as schedule}
                            {@const scheduleParties = parties.filter(p => p.scheduleId === schedule.id)}
                            <div class="card bg-base-100 shadow-md hover:shadow-lg transition-shadow border-l-4 {schedule.status === 'ACTIVE' ? 'border-success' : 'border-neutral'}">
                                <div class="card-body p-5 gap-4">

                                    <!-- Schedule header -->
                                    <div class="flex flex-col gap-2">
                                        <div class="flex items-start justify-between gap-2">
                                            <div class="min-w-0 flex-1">
                                                <div class="flex items-center gap-2 flex-wrap">
                                                    <h4 class="text-lg font-bold text-ubuntu">{schedule.title}</h4>
                                                    <span class="badge badge-sm {schedule.status === 'ACTIVE' ? 'badge-success text-white' : 'badge-neutral'}">{schedule.status}</span>
                                                </div>
                                                <p class="text-xs opacity-60 font-mono mt-1">
                                                    {new Date(schedule.start).toLocaleString()} — {new Date(schedule.end).toLocaleString()}
                                                </p>
                                            </div>
                                            {#if isAdmin}
                                                <div class="dropdown dropdown-end shrink-0">
                                                    <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
                                                    <!-- svelte-ignore a11y_label_has_associated_control -->
                                                    <label tabindex="0" class="btn btn-ghost btn-sm btn-circle">
                                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                                                            <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM12.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM18.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" />
                                                        </svg>
                                                    </label>
                                                    <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
                                                    <ul tabindex="0" class="dropdown-content z-10 menu p-2 shadow bg-base-100 rounded-box w-32 border border-base-200">
                                                        <li><button onclick={() => openRenameModal(schedule)}>Rename</button></li>
                                                        <li><button onclick={() => requestArchiveSchedule(schedule.id)}>Archive</button></li>
                                                        <li><button class="text-error" onclick={() => requestDeleteSchedule(schedule.id)}>Delete</button></li>
                                                    </ul>
                                                </div>
                                            {/if}
                                        </div>
                                        <!-- Action buttons: full row, wraps on mobile -->
                                        <div class="flex items-center gap-1 flex-wrap">
                                            <a href="{base}/schedules/availability?id={schedule.id}" class="btn btn-sm btn-outline">Submit Availability</a>
                                            <button class="btn btn-sm btn-primary" onclick={() => toggleHeatmap(schedule.id, schedule.start)}>
                                                {openHeatmapId === schedule.id ? 'Hide Heatmap' : 'View Heatmap'}
                                            </button>
                                        </div>
                                    </div>

                                    <!-- Heatmap -->
                                    {#if openHeatmapId === schedule.id && heatmapData}
                                        <div class="bg-base-200 rounded-lg border border-base-300 overflow-hidden">

                                            <!-- Desktop: days as rows, hours as columns -->
                                            <div class="hidden md:block overflow-x-auto p-2">
                                                <table class="table table-xs w-full text-center border-collapse">
                                                    <thead>
                                                        <tr>
                                                            <th class="border border-base-300 sticky left-0 bg-base-200 w-14 z-10"></th>
                                                            {#each Array(24) as _, i}
                                                                <th class="font-mono border border-base-300 text-[10px] w-5 px-0">{i}</th>
                                                            {/each}
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        {#each Array(8) as _, d}
                                                            <tr>
                                                                <th class="font-mono whitespace-nowrap px-1 py-1 border border-base-300 sticky left-0 bg-base-200 text-[10px] z-10" title={getFullDate(schedule.start, d)}>
                                                                    {formatDateOffset(schedule.start, d)}
                                                                </th>
                                                                {#each Array(24) as _, t}
                                                                    <!-- svelte-ignore a11y_click_events_have_key_events -->
                                                                    <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
                                                                    <td
                                                                        class="border border-base-300 p-0 cursor-pointer transition-colors {getHeatmapClass(heatmapData[d][t])}"
                                                                        title="{getFullDate(schedule.start, d)} {t}:00 — {heatmapData[d][t]} available"
                                                                        onclick={() => handleCellClick(d, t, schedule.start)}
                                                                    >
                                                                        <div class="w-full h-4 flex items-center justify-center text-[8px] font-bold">
                                                                            {heatmapData[d][t] > 0 ? heatmapData[d][t] : ''}
                                                                        </div>
                                                                    </td>
                                                                {/each}
                                                            </tr>
                                                        {/each}
                                                    </tbody>
                                                </table>
                                            </div>

                                            <!-- Mobile: hours as rows, days as columns -->
                                            <div class="md:hidden overflow-x-auto p-2">
                                                <table class="table table-xs w-full text-center border-collapse">
                                                    <thead>
                                                        <tr>
                                                            <th class="border border-base-300 sticky left-0 bg-base-200 w-8 z-10"></th>
                                                            {#each Array(8) as _, d}
                                                                <th class="font-mono px-1 py-1 border border-base-300 text-[10px] whitespace-nowrap" title={getFullDate(schedule.start, d)}>
                                                                    {formatDateOffset(schedule.start, d)}
                                                                </th>
                                                            {/each}
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        {#each Array(24) as _, t}
                                                            <tr>
                                                                <th class="font-mono px-0 py-1 border border-base-300 sticky left-0 bg-base-200 text-[10px] z-10">{t}</th>
                                                                {#each Array(8) as _, d}
                                                                    <!-- svelte-ignore a11y_click_events_have_key_events -->
                                                                    <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
                                                                    <td
                                                                        class="border border-base-300 p-0 cursor-pointer transition-colors {getHeatmapClass(heatmapData[d][t])}"
                                                                        title="{formatDateOffset(schedule.start, d)} {t}:00 — {heatmapData[d][t]} available"
                                                                        onclick={() => handleCellClick(d, t, schedule.start)}
                                                                    >
                                                                        <div class="w-full h-4 min-w-[2rem] flex items-center justify-center text-[8px] font-bold">
                                                                            {heatmapData[d][t] > 0 ? heatmapData[d][t] : ''}
                                                                        </div>
                                                                    </td>
                                                                {/each}
                                                            </tr>
                                                        {/each}
                                                    </tbody>
                                                </table>
                                            </div>

                                            <!-- Selected cell details (below table, not overlapping) -->
                                            {#if selectedCell}
                                                <div class="border-t border-base-300 bg-base-100 px-4 py-3 flex flex-wrap items-center gap-x-6 gap-y-2">
                                                    <div>
                                                        <p class="text-xs font-bold text-ubuntu">{getFullDate(schedule.start, selectedCell.d)}</p>
                                                        <p class="text-xs font-mono opacity-60">{selectedCell.t}:00 — {selectedCell.members.length} available</p>
                                                    </div>
                                                    <div class="flex flex-wrap gap-1 flex-1 min-w-0">
                                                        {#if selectedCell.members.length === 0}
                                                            <span class="text-xs opacity-50 italic">No one available at this time.</span>
                                                        {:else}
                                                            {#each selectedCell.members as name}
                                                                <span class="badge badge-sm badge-success">{name}</span>
                                                            {/each}
                                                        {/if}
                                                    </div>
                                                    <button class="btn btn-ghost btn-xs btn-circle ml-auto" onclick={() => selectedCell = null} aria-label="Close">✕</button>
                                                </div>
                                            {/if}
                                        </div>
                                    {/if}

                                    <!-- Parties section -->
                                    <div class="border-t border-base-300 pt-4">
                                        <div class="flex items-center justify-between mb-3">
                                            <h5 class="font-bold text-ubuntu">Parties</h5>
                                            {#if isAdmin}
                                                <a href="{base}/schedules/parties/create?id={schedule.id}" class="btn btn-xs btn-outline btn-secondary">Add Party</a>
                                            {/if}
                                        </div>

                                        {#if scheduleParties.length === 0}
                                            <p class="text-xs opacity-50 italic text-center py-3">No parties organized yet.</p>
                                        {:else}
                                            <div class="space-y-2">
                                                {#each scheduleParties as party}
                                                    {@const mySlots = party.joinedMembers.filter(pm => pm.userId === auth.user?.id)}
                                                    <div class="rounded-lg border transition-colors {expandedPartyId === party.id ? 'border-primary bg-base-200' : 'border-base-300 bg-base-200'}">

                                                        <!-- Party row header -->
                                                        <!-- svelte-ignore a11y_click_events_have_key_events -->
                                                        <!-- svelte-ignore a11y_no_static_element_interactions -->
                                                        <div
                                                            class="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-base-300 rounded-lg"
                                                            onclick={() => toggleParty(party.id, schedule.id)}
                                                        >
                                                            <div class="flex items-center gap-3 flex-1 min-w-0">
                                                                <span class="font-bold text-sm font-neo truncate">{party.title}</span>
                                                                <span class="text-xs opacity-60 font-mono shrink-0">{party.members}/{party.max}</span>
                                                                <span class="badge badge-sm {party.status === 'Done' ? 'badge-success text-white' : 'badge-outline'} hidden sm:flex">{party.status}</span>
                                                            </div>
                                                            <div class="flex items-center gap-2 shrink-0">
                                                                {#if party.status !== 'Done' && party.members < party.max && myCharacters.length > 0}
                                                                    <button
                                                                        class="btn btn-xs btn-primary"
                                                                        onclick={(e) => { e.stopPropagation(); openJoinModal(party, schedule.id); }}
                                                                    >Join</button>
                                                                {/if}
                                                                {#if isAdmin}
                                                                    <div class="dropdown dropdown-end" onclick={(e) => e.stopPropagation()}>
                                                                        <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
                                                                        <!-- svelte-ignore a11y_label_has_associated_control -->
                                                                        <label tabindex="0" class="btn btn-ghost btn-xs btn-circle">
                                                                            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                                                                                <path stroke-linecap="round" stroke-linejoin="round" d="M12 6.75a.75.75 0 110-1.5.75.75 0 010 1.5zM12 12.75a.75.75 0 110-1.5.75.75 0 010 1.5zM12 18.75a.75.75 0 110-1.5.75.75 0 010 1.5z" />
                                                                            </svg>
                                                                        </label>
                                                                        <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
                                                                        <ul tabindex="0" class="dropdown-content z-30 menu p-2 shadow bg-base-100 rounded-box w-36 border border-base-200">
                                                                            <li><button onclick={() => openEditModal(party, schedule.id)}>Edit</button></li>
                                                                            <li><button onclick={() => togglePartyStatus(party)}>{party.status === 'Done' ? 'Mark Planned' : 'Mark Done'}</button></li>
                                                                            <li><button class="text-error" onclick={() => requestDeleteParty(party)}>Delete</button></li>
                                                                        </ul>
                                                                    </div>
                                                                {/if}
                                                                {#if loadingPartyId === party.id}
                                                                    <span class="loading loading-spinner loading-xs text-primary"></span>
                                                                {:else}
                                                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4 transition-transform duration-200 {expandedPartyId === party.id ? 'rotate-180 text-primary' : 'opacity-40'}">
                                                                        <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                                                                    </svg>
                                                                {/if}
                                                            </div>
                                                        </div>

                                                        <!-- Party expanded details -->
                                                        {#if expandedPartyId === party.id}
                                                            <div class="px-4 pb-4 border-t border-base-300 bg-base-100 rounded-b-lg">
                                                                <div class="pt-4 grid grid-cols-1 sm:grid-cols-2 gap-4">
                                                                    <div>
                                                                        <p class="text-xs font-bold opacity-60 uppercase tracking-wider mb-1">Start Time</p>
                                                                        <p class="font-mono text-sm">{new Date(party.start).toLocaleString()}</p>
                                                                    </div>
                                                                    <div>
                                                                        <p class="text-xs font-bold opacity-60 uppercase tracking-wider mb-2">Members</p>
                                                                        <div class="flex flex-wrap gap-2">
                                                                            {#each party.joinedMembers as pm}
                                                                                <div class="flex items-center gap-1 badge badge-neutral py-3 pr-1 pl-2">
                                                                                    <div class="text-left leading-tight">
                                                                                        <span class="font-bold text-xs">{pm.characterName}</span>
                                                                                        {#if pm.characterClass}
                                                                                            <span class="text-[10px] opacity-60 ml-1">{pm.characterClass}</span>
                                                                                        {/if}
                                                                                        {#if pm.itemLevel}
                                                                                            <span class="text-[10px] opacity-50 ml-1">il{pm.itemLevel}</span>
                                                                                        {/if}
                                                                                    </div>
                                                                                    {#if (pm.userId === auth.user?.id || isAdmin) && party.status !== 'Done'}
                                                                                        <button
                                                                                            class="btn btn-ghost btn-xs btn-circle opacity-60 hover:opacity-100 hover:btn-error ml-1"
                                                                                            onclick={() => leaveParty(party.id, pm.characterId, schedule.id)}
                                                                                            title={pm.userId === auth.user?.id ? 'Leave with this character' : 'Remove from party'}
                                                                                        >✕</button>
                                                                                    {/if}
                                                                                </div>
                                                                            {/each}
                                                                            {#each Array(party.max - party.members) as _}
                                                                                <span class="badge badge-outline border-dashed opacity-40 py-3">Empty Slot</span>
                                                                            {/each}
                                                                        </div>
                                                                    </div>
                                                                </div>

                                                            </div>
                                                        {/if}
                                                    </div>
                                                {/each}
                                            </div>
                                        {/if}
                                    </div>

                                </div>
                            </div>
                        {/each}
                    {/if}
                </div>

                <!-- ── Sidebar ─────────────────────────────────────────── -->
                <div class="space-y-6">

                    <!-- My Characters card -->
                    <div class="card bg-base-100 shadow-xl">
                        <div class="card-body p-6">
                            <div class="flex items-center justify-between mb-4">
                                <h3 class="text-xl font-bold text-ubuntu">My Characters</h3>
                                {#if !showCharacterForm}
                                    <button class="btn btn-xs btn-outline btn-secondary" onclick={() => openCharacterForm()}>+ Add</button>
                                {/if}
                            </div>

                            {#if showCharacterForm}
                                <form class="space-y-3 mb-4 p-3 bg-base-200 rounded-lg" onsubmit={(e) => { e.preventDefault(); saveCharacter(); }}>
                                    <p class="text-sm font-bold">{editingCharacter ? 'Edit Character' : 'Register Character'}</p>
                                    <input type="text" class="input input-sm input-bordered w-full" placeholder="Character name*" bind:value={charName} required />
                                    <input type="text" class="input input-sm input-bordered w-full" placeholder="Class (e.g. Bard)" bind:value={charClass} />
                                    <input type="number" class="input input-sm input-bordered w-full" placeholder="Item level (e.g. 1620)" bind:value={charIlvl} step="0.01" min="0" />
                                    <div class="flex gap-2 justify-end">
                                        <button type="button" class="btn btn-xs btn-ghost" onclick={cancelCharacterForm}>Cancel</button>
                                        <button type="submit" class="btn btn-xs btn-primary" disabled={charFormLoading}>
                                            {#if charFormLoading}<span class="loading loading-spinner loading-xs"></span>{/if}
                                            Save
                                        </button>
                                    </div>
                                </form>
                            {/if}

                            {#if myCharacters.length === 0 && !showCharacterForm}
                                <p class="text-sm opacity-50 italic text-center py-4">No characters registered yet.</p>
                            {:else}
                                <div class="space-y-2">
                                    {#each myCharacters as char}
                                        <div class="flex items-center justify-between gap-2 p-2 rounded-lg bg-base-200">
                                            <div class="min-w-0">
                                                <p class="font-bold text-sm truncate">{char.name}</p>
                                                <p class="text-[11px] opacity-60">
                                                    {char.characterClass ?? 'Unknown class'}
                                                    {#if char.itemLevel} · il{char.itemLevel}{/if}
                                                </p>
                                            </div>
                                            <div class="flex gap-1 shrink-0">
                                                <button class="btn btn-ghost btn-xs btn-circle opacity-60 hover:opacity-100" onclick={() => openCharacterForm(char)} title="Edit">
                                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-3.5 h-3.5">
                                                        <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931z" />
                                                    </svg>
                                                </button>
                                                <button class="btn btn-ghost btn-xs btn-circle opacity-60 hover:opacity-100 hover:text-error" onclick={() => deleteCharacter(char)} title="Delete">
                                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-3.5 h-3.5">
                                                        <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                                                    </svg>
                                                </button>
                                            </div>
                                        </div>
                                    {/each}
                                </div>
                            {/if}
                        </div>
                    </div>

                    <!-- Group Roster card -->
                    <div class="card bg-base-100 shadow-xl">
                        <div class="card-body p-6">
                            <h3 class="text-xl font-bold text-ubuntu mb-4">Group Roster</h3>
                            <div class="space-y-3">
                                {#each members as member, i}
                                    <div class="flex items-center justify-between gap-2">
                                        <div class="flex items-center gap-3 min-w-0">
                                            <div class="avatar">
                                                <div class="w-9 rounded-full ring ring-base-300 ring-offset-base-100 ring-offset-1">
                                                    <img src={getAvatarUrl(member.discordId, member.avatar)} alt={member.username} />
                                                </div>
                                            </div>
                                            <div class="min-w-0">
                                                <p class="font-bold text-sm truncate">{member.username}</p>
                                                <p class="text-[10px] opacity-50 uppercase tracking-wider">{member.role}</p>
                                            </div>
                                        </div>

                                        {#if isAdmin && member.id !== auth.user?.id}
                                            <div class="dropdown dropdown-left {i >= members.length - 2 ? 'dropdown-top' : 'dropdown-bottom'}">
                                                <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
                                                <!-- svelte-ignore a11y_label_has_associated_control -->
                                                <label tabindex="0" class="btn btn-ghost btn-xs btn-circle opacity-50 hover:opacity-100">
                                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                                                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 6.75a.75.75 0 110-1.5.75.75 0 010 1.5zM12 12.75a.75.75 0 110-1.5.75.75 0 010 1.5zM12 18.75a.75.75 0 110-1.5.75.75 0 010 1.5z" />
                                                    </svg>
                                                </label>
                                                <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
                                                <ul tabindex="0" class="dropdown-content z-40 menu p-2 shadow bg-base-100 rounded-box w-36 border border-base-200">
                                                    <li><button onclick={() => notifyMember(member)}>Notify</button></li>
                                                    {#if isManager}
                                                        <li><button onclick={() => toggleAuditorRole(member)}>{member.role === 'AUDITOR' ? 'Revoke Auditor' : 'Grant Auditor'}</button></li>
                                                        <li><button class="text-error" onclick={() => requestKickMember(member)}>Kick</button></li>
                                                    {/if}
                                                </ul>
                                            </div>
                                        {/if}
                                    </div>
                                {/each}
                            </div>
                        </div>
                    </div>

                </div>

            </div>
        {/if}
    </div>
</main>

<!-- Join party modal -->
<dialog bind:this={joinModalDialog} class="modal">
    <div class="modal-box max-w-md">
        {#if joinModalParty}
            {@const p = joinModalParty.party}
            <!-- Header -->
            <div class="mb-5">
                <h3 class="font-bold text-lg text-ubuntu">{p.title}</h3>
                <div class="flex items-center gap-3 mt-1 text-sm opacity-60">
                    <span>{p.raidType}</span>
                    <span>·</span>
                    <span>{p.members}/{p.max} members</span>
                    {#if p.start}
                        <span>·</span>
                        <span class="font-mono">{new Date(p.start).toLocaleString()}</span>
                    {/if}
                </div>
            </div>

            <!-- Current members preview -->
            {#if p.joinedMembers.length > 0}
                <div class="mb-4">
                    <p class="text-xs font-bold opacity-50 uppercase tracking-wider mb-2">Current Members</p>
                    <div class="flex flex-wrap gap-1.5">
                        {#each p.joinedMembers as pm}
                            <span class="badge badge-neutral gap-1">
                                <span>{pm.characterName}</span>
                                {#if pm.characterClass}<span class="opacity-60">{pm.characterClass}</span>{/if}
                            </span>
                        {/each}
                        {#each Array(p.max - p.members) as _}
                            <span class="badge badge-outline border-dashed opacity-30">Empty</span>
                        {/each}
                    </div>
                </div>
            {/if}

            <div class="divider my-3"></div>

            <!-- Character selection -->
            <p class="text-sm font-bold mb-3">Choose a character to join with:</p>
            <div class="space-y-2 max-h-60 overflow-y-auto pr-1">
                {#each myCharacters as char}
                    <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
                    <!-- svelte-ignore a11y_click_events_have_key_events -->
                    <label
                        class="flex items-center gap-3 p-3 rounded-xl border-2 cursor-pointer transition-colors
                            {joinModalCharId === char.id ? 'border-primary bg-primary/5' : 'border-base-300 hover:border-base-400'}"
                        onclick={() => joinModalCharId = char.id}
                    >
                        <input type="radio" name="join-char" class="radio radio-primary radio-sm" value={char.id} bind:group={joinModalCharId} />
                        <div class="flex-1 min-w-0">
                            <p class="font-bold text-sm">{char.name}</p>
                            <p class="text-xs opacity-60">
                                {char.characterClass ?? 'Unknown class'}{char.itemLevel ? ` · il${char.itemLevel}` : ''}
                            </p>
                        </div>
                        {#if p.joinedMembers.some(pm => pm.characterId === char.id)}
                            <span class="badge badge-success badge-sm shrink-0">Joined</span>
                        {/if}
                    </label>
                {/each}
            </div>

            <div class="modal-action mt-5">
                <form method="dialog"><button class="btn btn-ghost">Cancel</button></form>
                <button
                    class="btn btn-primary"
                    disabled={!joinModalCharId || joinModalLoading || joinModalParty.party.joinedMembers.some(pm => pm.characterId === joinModalCharId)}
                    onclick={confirmJoin}
                >
                    {#if joinModalLoading}<span class="loading loading-spinner loading-xs"></span>{/if}
                    Join Party
                </button>
            </div>
        {/if}
    </div>
    <form method="dialog" class="modal-backdrop"><button>close</button></form>
</dialog>

<!-- Rename schedule modal -->
<dialog bind:this={renameModalDialog} class="modal">
    <div class="modal-box max-w-sm">
        {#if renameModalSchedule}
            <h3 class="font-bold text-lg text-ubuntu mb-4">Rename Schedule</h3>
            <div>
                <label class="block text-xs font-bold opacity-60 uppercase tracking-wider mb-1">Schedule Title *</label>
                <input
                    type="text"
                    class="input input-bordered w-full"
                    bind:value={renameTitle}
                    placeholder="e.g. March Week 1 Reset"
                    onkeydown={(e) => e.key === 'Enter' && submitRename()}
                />
            </div>
            <div class="modal-action mt-5">
                <form method="dialog"><button class="btn btn-ghost">Cancel</button></form>
                <button
                    class="btn btn-primary"
                    disabled={!renameTitle.trim() || renameTitle.trim() === renameModalSchedule.title || renameLoading}
                    onclick={submitRename}
                >
                    {#if renameLoading}<span class="loading loading-spinner loading-xs"></span>{/if}
                    Rename
                </button>
            </div>
        {/if}
    </div>
    <form method="dialog" class="modal-backdrop"><button>close</button></form>
</dialog>

<!-- Edit party modal -->
<dialog bind:this={editModalDialog} class="modal">
    <div class="modal-box max-w-sm">
        {#if editModalParty}
            {@const p = editModalParty.party}
            <h3 class="font-bold text-lg text-ubuntu mb-1">Edit Party</h3>
            <p class="text-xs opacity-50 font-mono mb-4">
                Start: {new Date(p.start).toLocaleString()}
            </p>
            <div class="space-y-3">
                <div>
                    <label class="block text-xs font-bold opacity-60 uppercase tracking-wider mb-1">Party Title *</label>
                    <input
                        type="text"
                        class="input input-bordered w-full"
                        bind:value={editTitle}
                        placeholder="e.g. Valtan Hard Fast Clear"
                    />
                </div>
                <div>
                    <label class="block text-xs font-bold opacity-60 uppercase tracking-wider mb-1">Raid Type (Optional)</label>
                    <input
                        type="text"
                        class="input input-bordered w-full"
                        bind:value={editRaidType}
                        placeholder="e.g. Valtan, Vykas, Argos"
                    />
                </div>
            </div>
            <div class="modal-action mt-5">
                <form method="dialog"><button class="btn btn-ghost">Cancel</button></form>
                <button
                    class="btn btn-primary"
                    disabled={!editTitle.trim() || editLoading}
                    onclick={submitEdit}
                >
                    {#if editLoading}<span class="loading loading-spinner loading-xs"></span>{/if}
                    Save
                </button>
            </div>
        {/if}
    </div>
    <form method="dialog" class="modal-backdrop"><button>close</button></form>
</dialog>

<!-- Schedule history modal -->
<dialog bind:this={historyModalDialog} class="modal">
    <div class="modal-box max-w-md">
        <h3 class="font-bold text-lg text-ubuntu mb-4">Schedule History</h3>
        {#if historyLoading}
            <div class="flex justify-center py-8">
                <span class="loading loading-spinner loading-md text-primary"></span>
            </div>
        {:else if archivedSchedules.length === 0}
            <p class="text-center opacity-60 py-8">No archived schedules.</p>
        {:else}
            <ul class="space-y-2">
                {#each archivedSchedules as s}
                    <li class="flex items-center gap-2 p-2 rounded-lg border border-base-200">
                        <a
                            href="{base}/schedules/view?id={s.id}"
                            class="flex flex-col gap-0.5 flex-1 min-w-0 hover:opacity-80 transition-opacity"
                            onclick={() => historyModalDialog.close()}
                        >
                            <span class="font-semibold text-ubuntu truncate">{s.title}</span>
                            <span class="text-xs opacity-60 font-mono">{new Date(s.start).toLocaleString()} — {new Date(s.end).toLocaleString()}</span>
                        </a>
                        {#if isAdmin}
                            <button
                                class="btn btn-xs btn-outline shrink-0"
                                onclick={() => unarchiveSchedule(s.id)}
                            >Restore</button>
                        {/if}
                    </li>
                {/each}
            </ul>
        {/if}
        <div class="modal-action mt-4">
            <form method="dialog"><button class="btn btn-ghost">Close</button></form>
        </div>
    </div>
    <form method="dialog" class="modal-backdrop"><button>close</button></form>
</dialog>

<ConfirmationModal
    bind:this={archiveScheduleModal}
    id="archive-schedule-modal"
    title="Archive Schedule"
    message="Archive this schedule? It will be hidden from the active list but can be viewed in History and restored at any time."
    confirmText="Archive"
    type="warning"
    onConfirm={confirmArchiveSchedule}
/>

<ConfirmationModal
    bind:this={deleteScheduleModal}
    id="delete-schedule-modal"
    title="Delete Schedule"
    message="Are you sure you want to delete this schedule? All member availability data and organized parties will be permanently removed."
    confirmText="Delete Schedule"
    type="error"
    onConfirm={confirmDeleteSchedule}
/>

<ConfirmationModal
    bind:this={deletePartyModal}
    id="delete-party-modal"
    title="Delete Party"
    message="Are you sure you want to delete this party? All joined members will be removed and the party will be permanently deleted."
    confirmText="Delete Party"
    type="error"
    onConfirm={confirmDeleteParty}
/>

<ConfirmationModal
    bind:this={kickMemberModal}
    id="kick-member-modal"
    title="Kick Member"
    message="Are you sure you want to remove {memberToKick?.username} from the group? They will be removed from all parties and their availability data will be deleted."
    confirmText="Kick Member"
    type="error"
    onConfirm={confirmKickMember}
/>
