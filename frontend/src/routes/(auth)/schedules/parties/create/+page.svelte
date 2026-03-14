<script lang="ts">
    import { onMount } from 'svelte';
    import { base } from '$app/paths';
    import { page } from '$app/stores';
    import { fetchApi, fetchJson, ApiError } from '$lib/api';
    import { auth } from '$lib/stores/auth.svelte';
    import { toast } from '$lib/stores/toast.svelte';
    import type { ScheduleResponse, GroupMemberResponse, AvailabilityResponse } from '$lib/types/api';

    let scheduleId = $derived($page.url.searchParams.get('id') ?? '');
    let schedule = $state<ScheduleResponse | null>(null);
    let members = $state<GroupMemberResponse[]>([]);
    let isLoading = $state(true);
    let title = $state('');
    let raidType = $state('');
    let maxMembers = $state(8);

    let heatmapData = $state<number[][] | null>(null);
    let rawAvailability = $state<AvailabilityResponse[]>([]);
    let selectedCell = $state<{ d: number; t: number; members: string[] } | null>(null);

    let isAdmin = $derived(
        members.find(m => m.id === auth.user?.id)?.role === 'MANAGER' ||
        members.find(m => m.id === auth.user?.id)?.role === 'AUDITOR'
    );

    const getInitialTime = () => {
        const now = new Date();
        now.setMinutes(0, 0, 0);
        return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    };
    let startTime = $state(getInitialTime());
    let isSubmitting = $state(false);

    async function fetchHeatmapData() {
        try {
            const availData = await fetchJson<AvailabilityResponse[]>(`/api/schedules/availability?scheduleId=${scheduleId}`);
            rawAvailability = availData;

            if (schedule?.start) {
                const grid = Array(8).fill(0).map(() => Array(24).fill(0));
                const baseDate = new Date(schedule.start);
                baseDate.setHours(0, 0, 0, 0);

                availData.forEach(userAvail => {
                    userAvail.blocks.forEach(block => {
                        const diffHoursTotal = Math.floor((new Date(block.start).getTime() - baseDate.getTime()) / 3600000);
                        const d = Math.floor(diffHoursTotal / 24);
                        const t = diffHoursTotal % 24;
                        if (d >= 0 && d < 8 && t >= 0 && t < 24) grid[d][t]++;
                    });
                });
                heatmapData = grid;
            }
        } catch (err) {
            console.error('Failed to fetch heatmap data:', err);
        }
    }

    function getAvailableMembers(d: number, t: number) {
        if (!schedule?.start) return [];
        const baseDate = new Date(schedule.start);
        baseDate.setHours(0, 0, 0, 0);
        const targetTime = baseDate.getTime() + (d * 24 + t) * 3600000;

        return rawAvailability
            .filter(ua => ua.blocks.some(b => new Date(b.start).getTime() === targetTime))
            .map(ua => members.find(m => m.id === ua.userId)?.username ?? 'Unknown');
    }

    function handleCellClick(d: number, t: number) {
        selectedCell = { d, t, members: getAvailableMembers(d, t) };
    }

    function formatDateOffset(dateString: string, offsetDays: number) {
        const d = new Date(dateString);
        d.setDate(d.getDate() + offsetDays);
        const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
        return `${String(d.getDate()).padStart(2, '0')} (${weekdays[d.getDay()]})`;
    }

    function getHeatmapClass(count: number) {
        if (!count) return 'bg-base-100';
        const ratio = count / Math.max(members.length, 1);
        if (ratio >= 0.8) return 'bg-success text-success-content';
        if (ratio >= 0.5) return 'bg-primary text-primary-content';
        if (ratio >= 0.2) return 'bg-info text-info-content';
        return 'bg-warning text-warning-content';
    }

    async function handleSubmit() {
        if (!title.trim() || !startTime) return;

        isSubmitting = true;
        try {
            await fetchApi(`/api/schedules/parties/create`, {
                method: 'POST',
                body: JSON.stringify({
                    scheduleId,
                    title,
                    raidType: raidType || 'General',
                    maxMembers,
                    startTime: new Date(startTime).toISOString()
                })
            });
            toast.success('Party created successfully!');
            window.history.back();
        } catch (err) {
            if (!(err instanceof ApiError && err.status === 401)) {
                toast.error('Failed to create party.');
            }
        } finally {
            isSubmitting = false;
        }
    }

    onMount(async () => {
        try {
            schedule = await fetchJson<ScheduleResponse>(`/api/schedules/detail?scheduleId=${scheduleId}`);

            if (schedule?.groupId) {
                members = await fetchJson<GroupMemberResponse[]>(`/api/members?groupId=${schedule.groupId}`);
            }
            await fetchHeatmapData();
        } catch (err) {
            console.error('Failed to load schedule context:', err);
        } finally {
            isLoading = false;
        }
    });
</script>

<svelte:head>
    <title>Create Party - Ark Resolver</title>
</svelte:head>

<main class="flex-1 p-4 md:p-8">
    <div class="container mx-auto max-w-4xl">
        <div class="flex items-center gap-4 mb-8">
            <button onclick={() => window.history.back()} class="btn btn-ghost btn-circle" aria-label="Go Back">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
            </button>
            <div>
                <h1 class="text-3xl font-bold text-ubuntu">Create Party</h1>
                {#if schedule}
                    <p class="opacity-70 font-neo text-sm italic">For: <span class="text-primary font-bold">{schedule.title}</span> ({new Date(schedule.start).toLocaleDateString()} ~ {new Date(schedule.end).toLocaleDateString()})</p>
                {:else}
                    <p class="opacity-70 font-neo text-sm">Organize a specific raid group.</p>
                {/if}
            </div>
        </div>

        <div class="space-y-8">
            <!-- Heatmap Reference -->
            <div class="card bg-base-100 shadow-xl border-t-4 border-info overflow-hidden">
                <div class="card-body relative p-0 sm:p-4">
                    <div class="p-4 sm:p-0">
                        <h3 class="card-title text-ubuntu">Heatmap Reference</h3>
                        <p class="text-sm opacity-70 mb-4">Click a cell to see who is available. Use this to pick the best start time.</p>
                    </div>

                    <div class="relative bg-base-200 rounded-lg overflow-hidden border border-base-300">
                        {#if !heatmapData}
                            <div class="w-full h-64 flex items-center justify-center italic opacity-50">
                                <span class="loading loading-spinner loading-md mr-2"></span>
                                Loading schedule availability...
                            </div>
                        {:else if schedule}
                            <div class="p-2 sm:p-4">
                                <!-- Desktop Grid -->
                                <div class="hidden md:block overflow-x-auto">
                                    <table class="table table-xs w-full text-center border-collapse">
                                        <thead>
                                            <tr>
                                                <th class="w-16"></th>
                                                {#each Array(24) as _, i}
                                                    <th class="font-mono text-[10px] w-6 opacity-60 p-0">{i}</th>
                                                {/each}
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {#each Array(8) as _, d}
                                                <tr>
                                                    <th class="font-mono whitespace-nowrap text-[10px] px-1 py-2 opacity-60 text-right">{formatDateOffset(schedule.start, d)}</th>
                                                    {#each Array(24) as _, t}
                                                        <!-- svelte-ignore a11y_click_events_have_key_events -->
                                                        <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
                                                        <td
                                                            class="border border-base-300 transition-colors cursor-pointer p-0 {getHeatmapClass(heatmapData[d][t])}"
                                                            onclick={() => handleCellClick(d, t)}
                                                        >
                                                            <div class="w-full h-6 text-[9px] flex items-center justify-center font-bold">
                                                                {heatmapData[d][t] > 0 ? heatmapData[d][t] : ''}
                                                            </div>
                                                        </td>
                                                    {/each}
                                                </tr>
                                            {/each}
                                        </tbody>
                                    </table>
                                </div>

                                <!-- Mobile Grid -->
                                <div class="md:hidden overflow-x-auto">
                                    <table class="table table-xs w-full text-center border-collapse">
                                        <thead>
                                            <tr>
                                                <th class="w-8"></th>
                                                {#each Array(8) as _, d}
                                                    <th class="font-mono text-[10px] opacity-60 p-1">{formatDateOffset(schedule.start, d).split(' ')[0]}</th>
                                                {/each}
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {#each Array(24) as _, t}
                                                <tr>
                                                    <th class="font-mono text-[10px] opacity-60 p-0">{t}</th>
                                                    {#each Array(8) as _, d}
                                                        <!-- svelte-ignore a11y_click_events_have_key_events -->
                                                        <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
                                                        <td
                                                            class="border border-base-300 transition-colors cursor-pointer p-0 {getHeatmapClass(heatmapData[d][t])}"
                                                            onclick={() => handleCellClick(d, t)}
                                                        >
                                                            <div class="w-full h-6 min-w-[2rem] text-[9px] flex items-center justify-center font-bold">
                                                                {heatmapData[d][t] > 0 ? heatmapData[d][t] : ''}
                                                            </div>
                                                        </td>
                                                    {/each}
                                                </tr>
                                            {/each}
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            <!-- Member Drawer Overlay -->
                            {#if selectedCell && schedule}
                                <div class="absolute inset-y-0 right-0 w-64 bg-base-100 shadow-2xl border-l border-base-300 z-20 flex flex-col">
                                    <div class="p-4 border-b border-base-300 flex justify-between items-center bg-base-200">
                                        <div>
                                            <p class="font-bold text-sm text-ubuntu">{formatDateOffset(schedule.start, selectedCell.d)}</p>
                                            <p class="text-xs opacity-60 font-mono">{selectedCell.t}:00 - Available ({selectedCell.members.length})</p>
                                        </div>
                                        <button class="btn btn-ghost btn-xs btn-circle" onclick={() => selectedCell = null}>✕</button>
                                    </div>
                                    <div class="flex-1 overflow-y-auto p-4">
                                        {#if selectedCell.members.length === 0}
                                            <p class="text-xs opacity-50 italic text-center py-8">No one is available at this time.</p>
                                        {:else}
                                            <ul class="space-y-2">
                                                {#each selectedCell.members as name}
                                                    <li class="flex items-center gap-2 text-sm">
                                                        <div class="w-2 h-2 rounded-full bg-success"></div>
                                                        <span class="font-neo">{name}</span>
                                                    </li>
                                                {/each}
                                            </ul>
                                        {/if}
                                    </div>
                                    <div class="p-4 bg-base-200 border-t border-base-300">
                                        <button
                                            class="btn btn-primary btn-sm w-full"
                                            onclick={() => {
                                                if (!schedule || !selectedCell) return;
                                                const d = new Date(schedule.start);
                                                d.setDate(d.getDate() + selectedCell.d);
                                                d.setHours(selectedCell.t, 0, 0, 0);
                                                startTime = new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
                                                selectedCell = null;
                                            }}
                                        >
                                            Use This Time
                                        </button>
                                    </div>
                                </div>
                            {/if}
                        {/if}
                    </div>
                </div>
            </div>

            <!-- Creation Form -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="space-y-6">
                        <div>
                            <label for="title" class="block text-sm font-bold text-ubuntu mb-2">Party Title</label>
                            <input
                                id="title"
                                type="text"
                                placeholder="e.g., Valtan Hard Fast Clear"
                                class="input input-bordered w-full font-neo focus:ring-primary"
                                bind:value={title}
                            />
                        </div>

                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div>
                                <label for="raid-type" class="block text-sm font-bold text-ubuntu mb-2">Content / Raid Type (Optional)</label>
                                <input
                                    id="raid-type"
                                    type="text"
                                    placeholder="e.g., Valtan, Vykas, Argos"
                                    class="input input-bordered w-full font-neo focus:ring-primary"
                                    bind:value={raidType}
                                />
                            </div>

                            <div>
                                <label for="max-members" class="block text-sm font-bold text-ubuntu mb-2">Max Members</label>
                                <div class="join w-full">
                                    <button class="btn join-item" onclick={() => maxMembers = Math.max(1, maxMembers - 1)}>-</button>
                                    <input
                                        id="max-members"
                                        type="number"
                                        class="input input-bordered join-item w-full text-center font-mono"
                                        bind:value={maxMembers}
                                        min="1"
                                        max="40"
                                    />
                                    <button class="btn join-item" onclick={() => maxMembers = Math.min(40, maxMembers + 1)}>+</button>
                                </div>
                            </div>
                        </div>

                        <div>
                            <label for="start-time" class="block text-sm font-bold text-ubuntu mb-2">Exact Start Time (Local Time)</label>
                            <input
                                id="start-time"
                                type="datetime-local"
                                class="input input-bordered w-full font-mono focus:ring-primary"
                                bind:value={startTime}
                            />
                            <span class="text-xs opacity-60 mt-1 block">Pick a time when most people are available based on the heatmap.</span>
                        </div>
                    </div>

                    <div class="card-actions justify-end mt-8">
                        <button class="btn btn-ghost" onclick={() => window.history.back()}>Cancel</button>
                        <button
                            class="btn btn-secondary px-8"
                            disabled={isSubmitting || !title.trim() || !startTime}
                            onclick={handleSubmit}
                        >
                            {#if isSubmitting}
                                <span class="loading loading-spinner"></span>
                            {/if}
                            Create Party
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>
