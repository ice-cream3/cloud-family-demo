import { useRef, useState } from 'react';
import type { PointerEvent } from 'react';
import type { AntiAbuseChallenge } from '../types/api';

type SliderChallengeProps = {
  challenge: AntiAbuseChallenge;
  disabled: boolean;
  onAnswer: (answer: string) => void;
};

export function SliderChallenge({ challenge, disabled, onAnswer }: SliderChallengeProps) {
  const [offset, setOffset] = useState(0);
  const [released, setReleased] = useState(false);
  const [dragging, setDragging] = useState(false);
  const drag = useRef<{ pointerId: number; startX: number; startOffset: number; travel: number } | null>(null);
  const slider = challenge.slider;
  if (!slider || !challenge.questionImage) {
    return <p className="auth-slider-hint">图片加载失败，请换一组。</p>;
  }

  function finishSlide(value: number) {
    setReleased(value > 0);
    onAnswer(value > 0 ? String(value) : '');
  }

  function cancelDrag() {
    if (!drag.current) return;
    drag.current = null;
    setDragging(false);
    setReleased(false);
    onAnswer('');
  }

  function dragOffset(event: PointerEvent<HTMLButtonElement>) {
    const active = drag.current;
    if (!active || !slider) return offset;
    const maximum = slider.width - slider.pieceSize;
    return Math.round(Math.max(0, Math.min(maximum,
      active.startOffset + (event.clientX - active.startX) / active.travel * maximum)));
  }

  return (
    <div className="auth-slider-challenge">
      <div className="auth-slider-picture" style={{ aspectRatio: `${slider.width} / ${slider.height}` }}>
        <img className="auth-slider-background" src={challenge.questionImage} alt="将拼图碎片移入图片缺口" draggable={false} />
        <img
          className="auth-slider-piece"
          src={slider.pieceImage}
          alt="待移动的拼图碎片"
          draggable={false}
          style={{
            width: `${slider.pieceSize / slider.width * 100}%`,
            left: `${offset / slider.width * 100}%`,
            top: `${slider.pieceY / slider.height * 100}%`,
          }}
        />
      </div>
      <div className="auth-slider-track">
        <span aria-hidden="true">{released ? '已调整位置，可继续微调' : '向右拖动滑块完成拼图'}</span>
        <button
          type="button"
          role="slider"
          className={`auth-slider-handle${dragging ? ' dragging' : ''}`}
          style={{ left: `calc(${offset / (slider.width - slider.pieceSize) * 100}% - ${offset / (slider.width - slider.pieceSize) * 40}px)` }}
          disabled={disabled}
          aria-label="向右拖动滑块完成拼图"
          aria-valuemin={0}
          aria-valuemax={slider.width - slider.pieceSize}
          aria-valuenow={offset}
          aria-valuetext={`已向右移动 ${offset} 像素`}
          onPointerDown={(event) => {
            if (disabled || drag.current || !event.isPrimary || event.button !== 0) return;
            const track = event.currentTarget.parentElement;
            if (!track) return;
            const travel = track.clientWidth - event.currentTarget.getBoundingClientRect().width;
            if (travel <= 0) return;
            event.preventDefault();
            event.currentTarget.focus();
            drag.current = { pointerId: event.pointerId, startX: event.clientX, startOffset: offset, travel };
            event.currentTarget.setPointerCapture(event.pointerId);
            setDragging(true);
            setReleased(false);
            onAnswer('');
          }}
          onPointerMove={(event) => {
            if (!drag.current || drag.current.pointerId !== event.pointerId) return;
            if (disabled || (event.pointerType === 'mouse' && (event.buttons & 1) === 0)) {
              cancelDrag();
              return;
            }
            setOffset(dragOffset(event));
          }}
          onPointerUp={(event) => {
            if (!drag.current || drag.current.pointerId !== event.pointerId) return;
            if (disabled) {
              cancelDrag();
              return;
            }
            const value = dragOffset(event);
            drag.current = null;
            setDragging(false);
            setOffset(value);
            finishSlide(value);
            event.currentTarget.releasePointerCapture(event.pointerId);
          }}
          onPointerCancel={cancelDrag}
          onLostPointerCapture={cancelDrag}
          onKeyDown={(event) => {
            if (disabled || drag.current) return;
            const maximum = slider.width - slider.pieceSize;
            const values: Record<string, number> = {
              ArrowLeft: offset - 1, ArrowDown: offset - 1,
              ArrowRight: offset + 1, ArrowUp: offset + 1,
              PageDown: offset - 10, PageUp: offset + 10, Home: 0, End: maximum,
            };
            if (!(event.key in values)) return;
            event.preventDefault();
            const value = Math.max(0, Math.min(maximum, values[event.key]));
            setOffset(value);
            finishSlide(value);
          }}
        ><span aria-hidden="true">›</span></button>
      </div>
      <p className="auth-slider-hint" role="status">{released ? '提交时验证拼图位置' : '按住滑块，将碎片移动到缺口后松开'}</p>
    </div>
  );
}
