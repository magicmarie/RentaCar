interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
  confirmLabel?: string;
}

export function ConfirmDialog({ open, title, message, onConfirm, onCancel, confirmLabel = 'OK' }: ConfirmDialogProps) {
  if (!open) return null;

  return (
    <div className="dialog-overlay" role="dialog" aria-modal="true">
      <div className="dialog-box">
        <h3>{title}</h3>
        <p>{message}</p>
        <div className="dialog-actions">
          <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
          <button type="button" className="btn btn-primary" onClick={onConfirm}>{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}
