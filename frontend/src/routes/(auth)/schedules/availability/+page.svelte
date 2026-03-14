<script lang="ts">
    import { onMount } from 'svelte';
    import { base } from '$app/paths';
    import { page } from '$app/stores';
    import { fetchApi, fetchJson, ApiError } from '$lib/api';
    import { toast } from '$lib/stores/toast.svelte';
    import type { ScheduleResponse, AvailabilityResponse } from '$lib/types/api';

    let scheduleId = $derived($page.url.searchParams.get('id') ?? '');
    let isSubmitting = $state(false);

    let schedule = $state<ScheduleResponse>({
        id: '',
        groupId: '',
        title: 'Loading...',
        start: new Date().toISOString(),
        end: new Date().toISOString(),
        status: 'ACTIVE'
    });

    let selectedCells = $state<boolean[][]>(Array(8).fill(null).map(() => Array(24).fill(false)));
    let isDragging = $state(false);
    let dragState = $state(false);
    let dragStart = $state<{ d: number; t: number } | null>(null);
    let dragCurrent = $state<{ d: number; t: number } | null>(null);

    function formatDateOffset(dateString: string, offsetDays: number) {
        const d = new Date(dateString);
        d.setDate(d.getDate() + offsetDays);
        const day = String(d.getDate()).padStart(2, '0');
        const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
        return `${day} (${weekdays[d.getDay()]})`;
    }

    function getFullDate(dateString: string, offsetDays: number) {
        const d = new Date(dateString);
        d.setDate(d.getDate() + offsetDays);
        return d.toLocaleDateString();
    }

    function isCellDisabled(d: number, t: number) {
        if (!schedule.start || !schedule.end) return false;
        const baseDate = new Date(schedule.start);
        baseDate.setHours(0, 0, 0, 0);
        const cellTime = baseDate.getTime() + (d * 24 + t) * 60 * 60 * 1000;
        return cellTime < new Date(schedule.start).getTime() || cellTime >= new Date(schedule.end).getTime();
    }

    function handleMouseDown(d: number, t: number) {
        if (isCellDisabled(d, t)) return;
        isDragging = true;
        dragState = !selectedCells[d][t];
        dragStart = { d, t };
        dragCurrent = { d, t };
    }

    function handleMouseEnter(d: number, t: number) {
        if (isDragging) dragCurrent = { d, t };
    }

    function applyDrag() {
        if (!dragStart || !dragCurrent) return;
        const minD = Math.min(dragStart.d, dragCurrent.d);
        const maxD = Math.max(dragStart.d, dragCurrent.d);
        const minT = Math.min(dragStart.t, dragCurrent.t);
        const maxT = Math.max(dragStart.t, dragCurrent.t);

        for (let d = minD; d <= maxD; d++) {
            for (let t = minT; t <= maxT; t++) {
                if (!isCellDisabled(d, t)) selectedCells[d][t] = dragState;
            }
        }
        selectedCells = [...selectedCells];
    }

    function handleMouseUp() {
        if (isDragging) applyDrag();
        isDragging = false;
        dragStart = null;
        dragCurrent = null;
    }

    // Touch support: get cell coords from element at touch point
    function getCellFromPoint(x: number, y: number): { d: number; t: number } | null {
        const el = document.elementFromPoint(x, y) as HTMLElement | null;
        if (!el) return null;
        const d = el.dataset.d !== undefined ? parseInt(el.dataset.d) : el.closest('[data-d]') ? parseInt((el.closest('[data-d]') as HTMLElement).dataset.d!) : NaN;
        const t = el.dataset.t !== undefined ? parseInt(el.dataset.t) : el.closest('[data-t]') ? parseInt((el.closest('[data-t]') as HTMLElement).dataset.t!) : NaN;
        if (isNaN(d) || isNaN(t)) return null;
        return { d, t };
    }

    function handleTouchStart(e: TouchEvent, d: number, t: number) {
        if (isCellDisabled(d, t)) return;
        e.preventDefault();
        isDragging = true;
        dragState = !selectedCells[d][t];
        dragStart = { d, t };
        dragCurrent = { d, t };
    }

    function handleTouchMove(e: TouchEvent) {
        if (!isDragging) return;
        e.preventDefault();
        const touch = e.touches[0];
        const cell = getCellFromPoint(touch.clientX, touch.clientY);
        if (cell) dragCurrent = cell;
    }

    function handleTouchEnd(e: TouchEvent) {
        if (isDragging) {
            e.preventDefault();
            applyDrag();
        }
        isDragging = false;
        dragStart = null;
        dragCurrent = null;
    }

    function isInDragBox(d: number, t: number) {
        if (!isDragging || !dragStart || !dragCurrent || isCellDisabled(d, t)) return false;
        const minD = Math.min(dragStart.d, dragCurrent.d);
        const maxD = Math.max(dragStart.d, dragCurrent.d);
        const minT = Math.min(dragStart.t, dragCurrent.t);
        const maxT = Math.max(dragStart.t, dragCurrent.t);
        return d >= minD && d <= maxD && t >= minT && t <= maxT;
    }

    function cellClass(d: number, t: number) {
        if (isCellDisabled(d, t)) return 'bg-base-200 cursor-not-allowed opacity-50';
        if (isInDragBox(d, t)) return dragState ? 'bg-primary/70 cursor-crosshair' : 'bg-base-300 cursor-crosshair';
        return selectedCells[d][t] ? 'bg-primary cursor-crosshair' : 'bg-base-100 hover:bg-base-300 cursor-crosshair';
    }

    async function loadData() {
        try {
            const s = await fetchJson<ScheduleResponse>(`/api/schedules/detail?scheduleId=${scheduleId}`);
            schedule = s;

            const myAvail = await fetchJson<AvailabilityResponse>(`/api/schedules/availability/me?scheduleId=${scheduleId}`);
            if (myAvail?.blocks && schedule.start) {
                const baseDate = new Date(schedule.start);
                baseDate.setHours(0, 0, 0, 0);
                selectedCells = Array(8).fill(null).map(() => Array(24).fill(false));

                myAvail.blocks.forEach(block => {
                    const diffHoursTotal = Math.floor((new Date(block.start).getTime() - baseDate.getTime()) / 3600000);
                    const d = Math.floor(diffHoursTotal / 24);
                    const t = diffHoursTotal % 24;
                    if (d >= 0 && d < 8 && t >= 0 && t < 24 && !isCellDisabled(d, t)) {
                        selectedCells[d][t] = true;
                    }
                });
                selectedCells = [...selectedCells];
            }
        } catch (err) {
            console.error('Failed to load availability:', err);
        }
    }

    async function handleSubmit() {
        isSubmitting = true;
        try {
            const baseDate = new Date(schedule.start);
            baseDate.setHours(0, 0, 0, 0);
            const blocks = [];

            for (let d = 0; d < 8; d++) {
                for (let t = 0; t < 24; t++) {
                    if (selectedCells[d][t]) {
                        const start = new Date(baseDate.getTime() + (d * 24 + t) * 3600000);
                        blocks.push({ start: start.toISOString(), end: new Date(start.getTime() + 3600000).toISOString() });
                    }
                }
            }

            await fetchApi(`/api/schedules/availability/me`, { method: 'POST', body: JSON.stringify({ scheduleId: scheduleId,  blocks  }) });
            toast.success('Availability saved!');
            window.history.back();
        } catch (err) {
            if (!(err instanceof ApiError && err.status === 401)) {
                toast.error('Failed to save. Syncing with server...');
                await loadData();
            }
        } finally {
            isSubmitting = false;
        }
    }

    onMount(loadData);
</script>

<svelte:head>
    <title>Submit Availability - Ark Resolver</title>
</svelte:head>

<svelte:window onmouseup={handleMouseUp} ontouchmove={handleTouchMove} ontouchend={handleTouchEnd} />

<main class="flex-1 p-4 md:p-8 select-none">
    <div class="container mx-auto max-w-4xl">
        <div class="flex items-center gap-4 mb-8">
            <button onclick={() => window.history.back()} class="btn btn-ghost btn-circle" aria-label="Go Back">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
            </button>
            <div>
                <h1 class="text-3xl font-bold text-ubuntu">Submit Availability</h1>
                <p class="opacity-70 font-neo text-sm">{schedule.title}</p>
            </div>
        </div>

        <div class="card bg-base-100 shadow-xl">
            <div class="card-body">
                <p class="text-sm opacity-80 mb-4 font-neo text-center">Click and drag to select or deselect the times you are available to raid.</p>

                <!-- Desktop Heatmap (Dates = Rows) -->
                <div class="hidden md:block overflow-x-auto bg-base-200 p-4 rounded-lg">
                    <table class="table table-xs w-full text-center border-collapse">
                        <thead>
                            <tr>
                                <th class="border-b border-base-300 w-16"></th>
                                {#each Array(24) as _, i}
                                    <th class="font-mono px-0 text-xs w-8 pb-2 opacity-60">{i}</th>
                                {/each}
                            </tr>
                        </thead>
                        <tbody>
                            {#each Array(8) as _, d}
                                <tr>
                                    <th class="font-mono whitespace-nowrap px-2 py-2 text-xs opacity-60 text-right pr-4" title={getFullDate(schedule.start, d)}>{formatDateOffset(schedule.start, d)}</th>
                                    {#each Array(24) as _, t}
                                        <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
                                        <td
                                            class="border border-base-300 transition-colors p-0 {cellClass(d, t)}"
                                            data-d={d}
                                            data-t={t}
                                            onmousedown={() => handleMouseDown(d, t)}
                                            onmouseenter={() => handleMouseEnter(d, t)}
                                            title={isCellDisabled(d, t) ? 'Outside schedule range' : `${formatDateOffset(schedule.start, d)} ${t}:00`}
                                        >
                                            <div class="w-full h-8"></div>
                                        </td>
                                    {/each}
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                </div>

                <!-- Mobile Heatmap (Dates = Columns) -->
                <div class="md:hidden overflow-x-auto bg-base-200 p-2 rounded-lg">
                    <table class="table table-xs w-full text-center border-collapse">
                        <thead>
                            <tr>
                                <th class="border-b border-base-300 w-8"></th>
                                {#each Array(8) as _, d}
                                    <th class="font-mono px-1 pb-2 text-xs opacity-60 whitespace-nowrap" title={getFullDate(schedule.start, d)}>{formatDateOffset(schedule.start, d)}</th>
                                {/each}
                            </tr>
                        </thead>
                        <tbody>
                            {#each Array(24) as _, t}
                                <tr>
                                    <th class="font-mono px-2 py-1 text-xs opacity-60 text-right pr-2">{t}</th>
                                    {#each Array(8) as _, d}
                                        <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
                                        <td
                                            class="border border-base-300 transition-colors p-0 {cellClass(d, t)}"
                                            data-d={d}
                                            data-t={t}
                                            onmousedown={() => handleMouseDown(d, t)}
                                            onmouseenter={() => handleMouseEnter(d, t)}
                                            ontouchstart={(e) => handleTouchStart(e, d, t)}
                                            title={isCellDisabled(d, t) ? 'Outside schedule range' : `${formatDateOffset(schedule.start, d)} ${t}:00`}
                                        >
                                            <div class="w-full h-8 min-w-[3rem]"></div>
                                        </td>
                                    {/each}
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                </div>

                <div class="flex justify-between items-center mt-8">
                    <button class="btn btn-outline btn-error" onclick={() => {
                        selectedCells = Array(8).fill(null).map(() => Array(24).fill(false));
                    }}>
                        Clear All
                    </button>
                    <div class="space-x-2">
                        <button class="btn btn-ghost" onclick={() => window.history.back()}>Cancel</button>
                        <button
                            class="btn btn-primary px-8"
                            disabled={isSubmitting}
                            onclick={handleSubmit}
                        >
                            {#if isSubmitting}
                                <span class="loading loading-spinner"></span>
                            {/if}
                            Save Availability
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>
