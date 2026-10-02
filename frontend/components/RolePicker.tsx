'use client';

import { useEffect, useMemo, useState } from 'react';
import { get } from '@/lib/api';
import type { Grade, RoleSummary } from '@/lib/types';

export type RoleChoice = { roleId: string; currentGrade: string; targetGrade: string };

export function RolePicker({ value, onChange }: { value: RoleChoice; onChange: (v: RoleChoice) => void }) {
  const [roles, setRoles] = useState<RoleSummary[]>([]);
  const [grades, setGrades] = useState<Grade[]>([]);

  useEffect(() => {
    get<RoleSummary[]>('/framework/roles').then(setRoles);
    get<Grade[]>('/framework/grades').then(setGrades);
  }, []);

  const byCapability = useMemo(() => {
    const m = new Map<string, RoleSummary[]>();
    roles.forEach((r) => m.set(r.capability, [...(m.get(r.capability) || []), r]));
    return m;
  }, [roles]);

  const currentIdx = grades.findIndex((g) => g.code === value.currentGrade);

  return (
    <div className="stack">
      <div className="field">
        <label htmlFor="role">Job role</label>
        <select id="role" value={value.roleId} onChange={(e) => onChange({ ...value, roleId: e.target.value })} required>
          <option value="">Choose your role…</option>
          {[...byCapability.entries()].map(([cap, list]) => (
            <optgroup key={cap} label={cap}>
              {list.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name} ({r.practice})
                </option>
              ))}
            </optgroup>
          ))}
        </select>
        <div className="hint">Roles and skills come from the DT billable skills Progression Framework.</div>
      </div>
      <div className="grid grid-2">
        <div className="field">
          <label htmlFor="cur">Current grade</label>
          <select
            id="cur"
            value={value.currentGrade}
            required
            onChange={(e) => {
              const idx = grades.findIndex((g) => g.code === e.target.value);
              const tIdx = grades.findIndex((g) => g.code === value.targetGrade);
              const target = tIdx <= idx ? grades[Math.min(idx + 1, grades.length - 1)]?.code : value.targetGrade;
              onChange({ ...value, currentGrade: e.target.value, targetGrade: target || e.target.value });
            }}
          >
            <option value="">Choose…</option>
            {grades.map((g) => (
              <option key={g.code} value={g.code}>
                {g.name} ({g.number})
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="tgt">Target grade</label>
          <select id="tgt" value={value.targetGrade} required onChange={(e) => onChange({ ...value, targetGrade: e.target.value })}>
            <option value="">Choose…</option>
            {grades.map((g, i) => (
              <option key={g.code} value={g.code} disabled={currentIdx >= 0 && i < currentIdx}>
                {g.name} ({g.number})
              </option>
            ))}
          </select>
          <div className="hint">Usually the next grade up, for your promotion.</div>
        </div>
      </div>
    </div>
  );
}
