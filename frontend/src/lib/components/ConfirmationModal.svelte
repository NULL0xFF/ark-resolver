<script lang="ts">
    import { type Snippet } from 'svelte';

    interface Props {
        id: string;
        title: string;
        message: string;
        confirmText?: string;
        cancelText?: string;
        onConfirm: () => void;
        onCancel?: () => void;
        type?: 'error' | 'warning' | 'info';
    }

    let { 
        id, 
        title, 
        message, 
        confirmText = 'Confirm', 
        cancelText = 'Cancel', 
        onConfirm, 
        onCancel,
        type = 'warning' 
    }: Props = $props();

    export function show() {
        const dialog = document.getElementById(id) as HTMLDialogElement;
        dialog?.showModal();
    }

    export function close() {
        const dialog = document.getElementById(id) as HTMLDialogElement;
        dialog?.close();
    }
</script>

<dialog {id} class="modal modal-bottom sm:modal-middle">
    <div class="modal-box border-t-4 {type === 'error' ? 'border-error' : type === 'warning' ? 'border-warning' : 'border-info'}">
        <h3 class="font-bold text-lg text-ubuntu">{title}</h3>
        <p class="py-4 font-neo text-sm opacity-80">{message}</p>
        <div class="modal-action">
            <form method="dialog" class="flex gap-2">
                <button class="btn btn-ghost" onclick={onCancel}>{cancelText}</button>
                <button 
                    class="btn {type === 'error' ? 'btn-error' : type === 'warning' ? 'btn-warning' : 'btn-info'}" 
                    onclick={(e) => {
                        e.preventDefault();
                        onConfirm();
                        close();
                    }}
                >
                    {confirmText}
                </button>
            </form>
        </div>
    </div>
</dialog>
