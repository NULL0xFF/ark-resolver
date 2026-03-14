<script lang="ts">
    import { toast } from '$lib/stores/toast.svelte';
</script>

<div class="toast toast-end toast-bottom z-[9999]">
    {#each toast.current as t (t.id)}
        <div class="alert shadow-lg animate-in fade-in slide-in-from-right duration-300
            {t.type === 'success' ? 'alert-success' : ''}
            {t.type === 'error' ? 'alert-error' : ''}
            {t.type === 'warning' ? 'alert-warning' : ''}
            {t.type === 'info' ? 'alert-info' : ''}">
            <span>{t.message}</span>
        </div>
    {/each}
</div>
