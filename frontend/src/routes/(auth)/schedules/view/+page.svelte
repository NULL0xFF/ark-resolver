<script lang="ts">
    import { onMount } from 'svelte';
    import { base } from '$app/paths';
    import { page } from '$app/stores';
    import { fetchJson } from '$lib/api';
    import { auth } from '$lib/stores/auth.svelte';
    import type { ScheduleResponse, PartyResponse, AvailabilityResponse, GroupMemberResponse } from '$lib/types/api';

    let scheduleId = $derived($page.url.searchParams.get('id') ?? '');

    let schedule = $state<ScheduleResponse | null>(null);
    let parties = $state<PartyResponse[]>([]);
    let rawAvailability = $state<AvailabilityResponse[]>([]);
    let members = $state<GroupMemberResponse[]>([]);
    let heatmapData = $state<number[][] | null>(null);
    let isLoading = $state(true);
    let error = $state('');
    let showHeatmap = $state(false);
    let selectedCell = $state<{ d: number; t: number; members: string[] } | null>(null);

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

    async function loadData() {
        try {
            const s = await fetchJson<ScheduleResponse>(`/api/schedules/detail?scheduleId=${scheduleId}`);
            schedule = s;

            const [partyData, availData, memberData] = await Promise.all([
                fetchJson<PartyResponse[]>(`/api/schedules/parties?scheduleId=${scheduleId}`),
                fetchJson<AvailabilityResponse[]>(`/api/schedules/availability?scheduleId=${scheduleId}`),
                fetchJson<GroupMemberResponse[]>(`/api/members?groupId=${s.groupId}`)
            ]);

            parties = partyData;
            rawAvailability = availData;
            members = memberData;

            const grid = Array.from({ length: 8 }, () => Array(24).fill(0));
            const baseDate = new Date(s.start);
            baseDate.setHours(0, 0, 0, 0);
            for (const userAvail of availData) {
                for (const block of userAvail.blocks) {
                    const diffHours = Math.floor((new Date(block.start).getTime() - baseDate.getTime()) / 3_600_000);
                    const d = Math.floor(diffHours / 24);
                    const t = diffHours % 24;
                    if (d >= 0 && d < 8 && t >= 0 && t < 24) grid[d][t]++;
                }
            }
            heatmapData = grid;
        } catch {
            error = 'Failed to load schedule.';
        } finally {
            isLoading = false;
        }
    }

    onMount(loadData);
</script>

<svelte:head>
    <title>{schedule ? schedule.title : 'Schedule'} - Ark Resolver</title>
</svelte:head>

<main class="flex-1 p-4 md:p-8">
    <div class="container mx-auto max-w-4xl">

        <!-- Back button + header -->
        <div class="flex items-center gap-4 mb-8">
            <button onclick={() => window.history.back()} class="btn btn-ghost btn-circle" aria-label="Go Back">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
                </svg>
            </button>
            <div>
                <div class="flex items-center gap-2">
                    <h1 class="text-3xl font-bold text-ubuntu">{schedule?.title ?? 'Loading...'}</h1>
                    <span class="badge badge-neutral">Archived</span>
                </div>
                {#if schedule}
                    <p class="text-xs opacity-60 font-mono mt-1">
                        {new Date(schedule.start).toLocaleString()} — {new Date(schedule.end).toLocaleString()}
                    </p>
                {/if}
            </div>
        </div>

        {#if isLoading}
            <div class="flex justify-center items-center h-64">
                <span class="loading loading-spinner loading-lg text-primary"></span>
            </div>

        {:else if error}
            <div class="alert alert-error shadow-lg">
                <span>{error}</span>
                <button onclick={() => window.history.back()} class="btn btn-sm btn-ghost">Go Back</button>
            </div>

        {:else if schedule}
            <!-- Parties -->
            <div class="space-y-4 mb-8">
                <h2 class="text-xl font-bold text-ubuntu">Parties</h2>
                {#if parties.length === 0}
                    <div class="card bg-base-100 shadow-md">
                        <div class="card-body text-center opacity-60 py-8">No parties were organized for this schedule.</div>
                    </div>
                {:else}
                    {#each parties as party}
                        <div class="card bg-base-100 shadow-md border-l-4 {party.status === 'Done' ? 'border-success' : 'border-neutral'}">
                            <div class="card-body p-4 gap-2">
                                <div class="flex items-center justify-between gap-2">
                                    <div>
                                        <div class="flex items-center gap-2 flex-wrap">
                                            <h3 class="font-bold text-ubuntu">{party.title}</h3>
                                            {#if party.raidType}
                                                <span class="badge badge-sm badge-outline">{party.raidType}</span>
                                            {/if}
                                            <span class="badge badge-sm {party.status === 'Done' ? 'badge-success text-white' : 'badge-neutral'}">{party.status}</span>
                                        </div>
                                        <p class="text-xs opacity-60 font-mono">{new Date(party.start).toLocaleString()} · {party.members}/{party.max} members</p>
                                    </div>
                                </div>

                                {#if party.joinedMembers.length > 0}
                                    <div class="flex flex-wrap gap-2 mt-1">
                                        {#each party.joinedMembers as pm}
                                            <div class="badge badge-outline gap-1 text-xs py-3">
                                                <span class="font-semibold">{pm.characterName}</span>
                                                {#if pm.characterClass}
                                                    <span class="opacity-60">· {pm.characterClass}</span>
                                                {/if}
                                                {#if pm.itemLevel}
                                                    <span class="opacity-60">· {pm.itemLevel}</span>
                                                {/if}
                                                {#if pm.userId === auth.user?.id}
                                                    <span class="text-primary font-bold">(me)</span>
                                                {/if}
                                            </div>
                                        {/each}
                                    </div>
                                {/if}
                            </div>
                        </div>
                    {/each}
                {/if}
            </div>

            <!-- Availability heatmap -->
            <div class="space-y-4">
                <div class="flex items-center justify-between">
                    <h2 class="text-xl font-bold text-ubuntu">Availability</h2>
                    <button class="btn btn-sm btn-primary" onclick={() => { showHeatmap = !showHeatmap; selectedCell = null; }}>
                        {showHeatmap ? 'Hide Heatmap' : 'View Heatmap'}
                    </button>
                </div>

                {#if showHeatmap && heatmapData}
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
                                    {#each heatmapData as row, d}
                                        <tr>
                                            <th class="font-mono text-xs border border-base-300 sticky left-0 bg-base-200 px-1 whitespace-nowrap" title={getFullDate(schedule.start, d)}>
                                                {formatDateOffset(schedule.start, d)}
                                            </th>
                                            {#each row as count, t}
                                                <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
                                                <td
                                                    class="border border-base-300 w-5 h-5 p-0 cursor-pointer transition-opacity hover:opacity-70 {getHeatmapClass(count)}"
                                                    title="{count} member{count !== 1 ? 's' : ''} available"
                                                    onclick={() => handleCellClick(d, t, schedule!.start)}
                                                >
                                                    <div class="w-full h-5 text-[9px] flex items-center justify-center">{count || ''}</div>
                                                </td>
                                            {/each}
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>

                        <!-- Mobile: dates as columns, hours as rows -->
                        <div class="md:hidden overflow-x-auto p-2">
                            <table class="table table-xs w-full text-center border-collapse">
                                <thead>
                                    <tr>
                                        <th class="border border-base-300 sticky left-0 bg-base-200 w-8 z-10"></th>
                                        {#each heatmapData as _, d}
                                            <th class="font-mono border border-base-300 text-[10px] px-1 whitespace-nowrap" title={getFullDate(schedule.start, d)}>
                                                {formatDateOffset(schedule.start, d)}
                                            </th>
                                        {/each}
                                    </tr>
                                </thead>
                                <tbody>
                                    {#each Array(24) as _, t}
                                        <tr>
                                            <th class="font-mono text-xs border border-base-300 sticky left-0 bg-base-200 px-1">{t}</th>
                                            {#each heatmapData as row, d}
                                                <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
                                                <td
                                                    class="border border-base-300 w-8 h-5 p-0 cursor-pointer transition-opacity hover:opacity-70 {getHeatmapClass(row[t])}"
                                                    title="{row[t]} member{row[t] !== 1 ? 's' : ''} available"
                                                    onclick={() => handleCellClick(d, t, schedule!.start)}
                                                >
                                                    <div class="w-full h-5 text-[9px] flex items-center justify-center">{row[t] || ''}</div>
                                                </td>
                                            {/each}
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>

                        {#if selectedCell}
                            <div class="px-4 pb-3 pt-1 border-t border-base-300 text-sm">
                                <span class="font-semibold">{getFullDate(schedule.start, selectedCell.d)} {selectedCell.t}:00</span>
                                {#if selectedCell.members.length === 0}
                                    <span class="opacity-60 ml-2">No one available</span>
                                {:else}
                                    <span class="opacity-60 ml-2">— {selectedCell.members.join(', ')}</span>
                                {/if}
                            </div>
                        {/if}
                    </div>
                {/if}
            </div>
        {/if}
    </div>
</main>
