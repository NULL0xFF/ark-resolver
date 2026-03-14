export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
    id: number;
    message: string;
    type: ToastType;
}

let toasts = $state<Toast[]>([]);
let counter = 0;

export const toast = {
    get current() { return toasts; },
    
    push(message: string, type: ToastType = 'info', duration = 3000) {
        const id = counter++;
        toasts.push({ id, message, type });
        
        setTimeout(() => {
            toasts = toasts.filter(t => t.id !== id);
        }, duration);
    },
    
    success(message: string) { this.push(message, 'success'); },
    error(message: string) { this.push(message, 'error'); },
    info(message: string) { this.push(message, 'info'); },
    warning(message: string) { this.push(message, 'warning'); }
};
