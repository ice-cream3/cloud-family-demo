import { useCallback, useEffect, useRef, useState } from 'react';
import { createManagerLoginChallenge } from '../services/authService';
import type { AntiAbuseChallenge } from '../types/api';
import { SliderChallenge } from './SliderChallenge';

export type ChallengeAnswer = { challengeId: string; answer: string };

type LoginChallengeProps = {
  disabled: boolean;
  revision: number;
  onChange: (answer: ChallengeAnswer | null) => void;
};

export function LoginChallenge({ disabled, revision, onChange }: LoginChallengeProps) {
  const [challenge, setChallenge] = useState<AntiAbuseChallenge | null>(null);
  const [answer, setAnswer] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [draggedValue, setDraggedValue] = useState<string | null>(null);
  const requestSequence = useRef(0);
  const drag = useRef<{ value: string; pointerId: number } | null>(null);

  const reload = useCallback(async () => {
    const sequence = ++requestSequence.current;
    setLoading(true);
    setChallenge(null);
    setAnswer('');
    setError(null);
    setDraggedValue(null);
    drag.current = null;
    onChange(null);
    try {
      const next = await createManagerLoginChallenge();
      if (sequence !== requestSequence.current) return;
      setChallenge(next);
      const initial = next.scheme === 'image_reassemble'
        ? (next.options || []).map((option) => option.value).join(',') : '';
      setAnswer(initial);
      if (initial && initial.split(',').length === next.requiredSelectionCount) {
        onChange({ challengeId: next.challengeId, answer: initial });
      }
    } catch {
      if (sequence === requestSequence.current) setError('人机验证加载失败，请点击“换一组”重试。');
    } finally {
      if (sequence === requestSequence.current) setLoading(false);
    }
  }, [onChange]);

  useEffect(() => {
    const requests = requestSequence;
    void reload();
    return () => { requests.current++; };
  }, [reload, revision]);

  useEffect(() => {
    if (!challenge) return;
    const timer = window.setTimeout(() => { void reload(); }, challenge.expiresInSeconds * 1000);
    return () => window.clearTimeout(timer);
  }, [challenge, reload]);

  const values = answer ? answer.split(',').filter(Boolean) : [];
  const required = challenge?.requiredSelectionCount || 3;
  const puzzle = challenge?.scheme === 'image_reassemble';
  const options = puzzle
    ? values.flatMap((value) => challenge?.options?.filter((option) => option.value === value) || [])
    : challenge?.options || [];

  function updateAnswer(next: string) {
    setAnswer(next);
    const complete = next.split(',').filter(Boolean).length === required;
    onChange(challenge && complete ? { challengeId: challenge.challengeId, answer: next } : null);
  }

  function select(value: string) {
    if (disabled || loading || puzzle) return;
    const next = values.includes(value) ? values.filter((item) => item !== value) : [...values, value].slice(0, required);
    updateAnswer(next.join(','));
  }

  function stopDrag() {
    drag.current = null;
    setDraggedValue(null);
  }

  function swap(fromValue: string, toValue: string) {
    if (fromValue === toValue || disabled || loading) return;
    const from = values.indexOf(fromValue);
    const to = values.indexOf(toValue);
    if (from < 0 || to < 0) return;
    const next = [...values];
    [next[from], next[to]] = [next[to], next[from]];
    updateAnswer(next.join(','));
  }

  return (
    <section className="manager-challenge" aria-label="人机验证" aria-busy={loading}>
      <div className="manager-challenge-heading">
        <strong>人机验证</strong>
        <button type="button" onClick={() => { void reload(); }} disabled={disabled || loading}>
          {loading ? '加载中' : '换一组'}
        </button>
      </div>
      {error ? <p role="alert" className="manager-challenge-error">{error}</p> : null}
      {loading ? <p role="status" className="manager-challenge-hint">正在加载验证图片…</p> : null}
      {challenge?.scheme === 'image_slider' ? (
        <SliderChallenge key={challenge.challengeId} challenge={challenge} disabled={disabled || loading} onAnswer={updateAnswer} />
      ) : challenge ? (
        <>
          {challenge.questionImage ? (
            <img className={`manager-challenge-question${puzzle ? ' puzzle' : ''}`}
              src={challenge.questionImage} alt={challenge.question} draggable={false} />
          ) : <p>{challenge.question}</p>}
          <div className={`manager-challenge-options${puzzle ? ' puzzle-board' : ''}`} role="group"
            aria-label={puzzle ? '按住图片拖到另一块图片上交换位置' : challenge.question}>
            {options.map((option, index) => (
              <button key={option.value} type="button" disabled={disabled || loading}
                className={`${!puzzle && values.includes(option.value) ? 'selected' : ''}${draggedValue === option.value ? ' dragging' : ''}`}
                onClick={() => select(option.value)}
                role={puzzle ? undefined : 'checkbox'}
                aria-checked={puzzle ? undefined : values.includes(option.value)}
                aria-label={puzzle ? `第${index + 1}块图片，可拖拽交换位置` : `图形选项${index + 1}`}
                data-manager-puzzle-value={puzzle ? option.value : undefined}
                onPointerDown={(event) => {
                  if (!puzzle || disabled || loading || drag.current || !event.isPrimary || event.button !== 0) return;
                  event.preventDefault();
                  drag.current = { value: option.value, pointerId: event.pointerId };
                  setDraggedValue(option.value);
                  event.currentTarget.setPointerCapture(event.pointerId);
                }}
                onPointerMove={(event) => {
                  if (drag.current?.pointerId === event.pointerId && event.pointerType === 'mouse' && (event.buttons & 1) === 0) stopDrag();
                }}
                onPointerUp={(event) => {
                  if (!drag.current || drag.current.pointerId !== event.pointerId) return;
                  const target = document.elementFromPoint(event.clientX, event.clientY)
                    ?.closest<HTMLElement>('[data-manager-puzzle-value]');
                  if (target?.dataset.managerPuzzleValue && event.currentTarget.closest('.manager-challenge')?.contains(target)) {
                    swap(drag.current.value, target.dataset.managerPuzzleValue);
                  }
                  stopDrag();
                  event.currentTarget.releasePointerCapture(event.pointerId);
                }}
                onPointerCancel={stopDrag} onLostPointerCapture={stopDrag}>
                {option.image ? <img src={option.image} alt="拼图碎片" draggable={false} /> : (
                  <svg viewBox="0 0 100 100" aria-hidden="true">
                    <polygon points={option.shape?.clipPath.replaceAll('%', '')}
                      fill={option.fillMode === 'outline' ? 'none' : option.color}
                      stroke={option.fillMode === 'outline' ? option.color : 'none'} strokeWidth="7" strokeLinejoin="round" />
                  </svg>
                )}
                {!puzzle && values.includes(option.value) ? <span className="manager-challenge-order">{values.indexOf(option.value) + 1}</span> : null}
              </button>
            ))}
          </div>
          <p className="manager-challenge-hint" role="status">
            {puzzle ? '按住图片拖到另一块图片上，松开后交换位置' : `已选择 ${values.length}/${required} 个图形`}
          </p>
        </>
      ) : null}
    </section>
  );
}
