import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { AlertTriangle, ArrowRight, LoaderCircle, UserRound, X } from 'lucide-react';
import type { TripSlot } from '../types/api';

type TripNoShowDialogProps = {
  slot: TripSlot;
  penalty: number;
  submitting: boolean;
  error: string | null;
  onClose: () => void;
  onConfirm: () => void;
};

export function TripNoShowDialog({ slot, penalty, submitting, error, onClose, onConfirm }: TripNoShowDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);
  const applicant = slot.applicantDisplayName || slot.applicantUsername || '预约人';

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    const previousOverflow = document.body.style.overflow;
    dialog.showModal();
    document.body.style.overflow = 'hidden';
    cancelRef.current?.focus();
    return () => {
      dialog.close();
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return createPortal(
    <dialog ref={dialogRef} className="trip-no-show-dialog" aria-labelledby="trip-no-show-title"
      aria-describedby="trip-no-show-description" aria-busy={submitting}
      onCancel={(event) => {
        event.preventDefault();
        if (!submitting) onClose();
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget && !submitting) onClose();
      }}>
      <div className="trip-no-show-content">
        <button className="trip-no-show-close" type="button" aria-label="关闭确认弹窗"
          onClick={onClose} disabled={submitting}><X size={18} /></button>
        <div className="trip-no-show-icon" aria-hidden="true"><AlertTriangle size={25} /></div>
        <h2 id="trip-no-show-title">确认标记未履约？</h2>
        <p id="trip-no-show-description" className="trip-no-show-description">
          请确认对方确实未按约定履约，<br />此操作将扣除对方的信用分。
        </p>

        <div className="trip-no-show-summary">
          <div className="trip-no-show-person">
            <span className="trip-no-show-avatar" aria-hidden="true"><UserRound size={20} /></span>
            <div><strong>{applicant}</strong><span>预约人</span></div>
          </div>
          <div className="trip-no-show-trip">
            <strong>{slot.title}</strong>
            <span>{slot.tripDate} · {slot.startTime.slice(0, 5)}–{slot.endTime.slice(0, 5)}</span>
          </div>
          <div className="trip-no-show-penalty"><span>本次信用分扣减</span><strong>−{penalty}<small> 分</small></strong></div>
        </div>

        {error ? <p className="trip-no-show-error" role="alert">{error}</p> : null}
        <div className="trip-no-show-actions">
          <button ref={cancelRef} type="button" className="trip-no-show-cancel" onClick={onClose} disabled={submitting}>暂不标记</button>
          <button type="button" className="trip-no-show-confirm" onClick={onConfirm} disabled={submitting}>
            {submitting ? <><LoaderCircle className="trip-no-show-spinner" size={16} />处理中</> : <>确认标记<ArrowRight size={16} /></>}
          </button>
        </div>
      </div>
    </dialog>, document.body,
  );
}
