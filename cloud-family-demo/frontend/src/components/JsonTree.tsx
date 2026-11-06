import { ChevronRight } from 'lucide-react';

export function JsonTree({ data }: { data: unknown }) {
  return <div className="json-tree" aria-label="JSON 数据，可展开或折叠对象和数组"><JsonNode value={data} root /></div>;
}

function JsonNode({ value, name, comma = false, root = false }: {
  value: unknown; name?: string; comma?: boolean; root?: boolean;
}) {
  const key = name === undefined ? null : <><span className="json-key">{JSON.stringify(name)}</span><span className="json-punctuation">: </span></>;
  const suffix = comma ? ',' : '';
  if (value !== null && typeof value === 'object') {
    const array = Array.isArray(value);
    const entries = array ? value.map((item, index) => [String(index), item] as const) : Object.entries(value);
    const opening = array ? '[' : '{';
    const closing = array ? ']' : '}';
    if (!entries.length) return <div className="json-leaf">{key}<span className="json-punctuation">{opening}{closing}{suffix}</span></div>;
    return <details className="json-branch" open={root}>
      <summary aria-label={`${name ?? (array ? '数组' : '对象')}，${entries.length}${array ? ' 项' : ' 个属性'}`}>
        <ChevronRight size={13} aria-hidden="true" />
        <span className="json-summary-value">{key}<span className="json-punctuation">{opening}<span className="json-collapsed-preview">…{closing}{suffix}</span></span></span>
        <small>{entries.length} {array ? '项' : '个属性'}</small>
      </summary>
      <div className="json-children">
        {entries.map(([entryKey, entryValue], index) => <JsonNode key={entryKey} name={array ? undefined : entryKey} value={entryValue} comma={index < entries.length - 1} />)}
      </div>
      <div className="json-closing json-punctuation">{closing}{suffix}</div>
    </details>;
  }
  const type = value === null ? 'null' : typeof value;
  const text = type === 'string' ? JSON.stringify(value) : String(value);
  return <div className="json-leaf">{key}<span className={`json-value json-${type}`}>{text}</span><span className="json-punctuation">{suffix}</span></div>;
}
