import { useEffect, useId, useRef, useState } from 'react';
import { Check, ChevronDown } from 'lucide-react';
import type { MerchantType } from '../services/commerceService';

type Props = {
  options: MerchantType[];
  value: string;
  disabled?: boolean;
  onChange: (value: string) => void;
};

export function ProductTypePicker({ options, value, disabled, onChange }: Props) {
  const id = useId();
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const selectedIndex = options.findIndex((option) => option.typeCode === value);
  const unavailable = Boolean(disabled || options.length === 0);
  const expanded = open && !unavailable;
  const selected = options[selectedIndex];
  const text = selected?.name || (value ? '原类型已失效，请重新选择' : options.length ? '请选择商品类型' : '暂无可用类型');

  useEffect(() => {
    if (!expanded) return;
    listRef.current?.focus();
    function outside(event: PointerEvent) {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    }
    document.addEventListener('pointerdown', outside);
    return () => document.removeEventListener('pointerdown', outside);
  }, [expanded]);

  useEffect(() => {
    if (expanded) document.getElementById(`${id}-option-${activeIndex}`)?.scrollIntoView({ block: 'nearest' });
  }, [expanded, activeIndex, id]);

  function show(startAtEnd = false) {
    if (unavailable) return;
    setActiveIndex(selectedIndex >= 0 ? selectedIndex : startAtEnd ? options.length - 1 : 0);
    setOpen(true);
  }

  function close() {
    setOpen(false);
    triggerRef.current?.focus();
  }

  function choose(index: number) {
    const option = options[index];
    if (!option) return;
    onChange(option.typeCode);
    close();
  }

  return <div ref={containerRef} className="seller-product-type-field">
    <span id={`${id}-label`} className="product-type-label">商品类型</span>
    <div className={`product-type-picker ${expanded ? 'is-open' : ''}`}>
      <button ref={triggerRef} type="button" className={`product-type-trigger ${!selected ? 'is-placeholder' : ''}`}
        disabled={unavailable} aria-haspopup="listbox" aria-expanded={expanded}
        aria-controls={expanded ? `${id}-list` : undefined} aria-labelledby={`${id}-label ${id}-value`}
        aria-describedby={`${id}-hint`} onClick={() => expanded ? close() : show()}
        onKeyDown={(event) => {
          if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault(); show(event.key === 'ArrowUp');
          }
        }}>
        <span id={`${id}-value`}>{text}</span><ChevronDown size={18} aria-hidden="true" />
      </button>
      {expanded ? <div ref={listRef} id={`${id}-list`} className="product-type-menu" role="listbox" aria-labelledby={`${id}-label`}
        aria-required="true" tabIndex={-1} aria-activedescendant={`${id}-option-${activeIndex}`}
        onKeyDown={(event) => {
          if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); close(); }
          else if (event.key === 'Tab') setOpen(false);
          else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault();
            setActiveIndex((index) => (index + (event.key === 'ArrowDown' ? 1 : -1) + options.length) % options.length);
          } else if (event.key === 'Home' || event.key === 'End') {
            event.preventDefault(); setActiveIndex(event.key === 'Home' ? 0 : options.length - 1);
          } else if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault(); choose(activeIndex);
          }
        }}>
        <div className="product-type-menu-heading" aria-hidden="true">选择经营类型</div>
        {options.map((option, index) => <div key={option.typeCode} id={`${id}-option-${index}`} role="option"
          aria-selected={option.typeCode === value}
          className={`product-type-option ${index === activeIndex ? 'is-active' : ''} ${option.typeCode === value ? 'is-selected' : ''}`}
          onMouseEnter={() => setActiveIndex(index)} onClick={() => choose(index)}>
          <span>{option.name}{options.some((candidate) => candidate.typeCode !== option.typeCode && candidate.name === option.name) ? <small>{option.typeCode}</small> : null}</span>
          {option.typeCode === value ? <Check size={17} aria-hidden="true" /> : null}
        </div>)}
      </div> : null}
    </div>
    <small id={`${id}-hint`}>可选范围为店铺已开通的经营类型</small>
  </div>;
}
