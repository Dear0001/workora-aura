'use client';
import React from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

import type { DashboardSummary } from '@/lib/api/types';

interface TooltipProps {
  active?: boolean;
  payload?: Array<{ name: string; value: number; color: string }>;
  label?: string;
}

function CustomTooltip({ active, payload, label }: TooltipProps) {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-card border border-border rounded-lg shadow-elevated px-3 py-2.5 text-xs">
      <p className="font-semibold text-foreground mb-1.5">{label}</p>
      {payload.map((entry) => (
        <div key={`tooltip-${entry.name}`} className="flex items-center gap-2 mb-0.5">
          <span className="w-2 h-2 rounded-full flex-shrink-0" style={{ backgroundColor: entry.color }} />
          <span className="text-muted-foreground capitalize">{entry.name}:</span>
          <span className="font-semibold text-foreground font-tabular">{entry.value}</span>
        </div>
      ))}
    </div>
  );
}

export default function TaskTrendChart({ data }: { data: DashboardSummary['taskActivity'] }) {
  if (data.length === 0) {
    return <div className="h-[220px] flex items-center justify-center text-sm text-muted-foreground">No task activity available.</div>;
  }

  return (
    <ResponsiveContainer width="100%" height={220}>
      <AreaChart data={data} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
        <defs>
          <linearGradient id="completedGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="var(--primary)" stopOpacity={0.2} />
            <stop offset="95%" stopColor="var(--primary)" stopOpacity={0} />
          </linearGradient>
          <linearGradient id="createdGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#7C3AED" stopOpacity={0.15} />
            <stop offset="95%" stopColor="#7C3AED" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
        <XAxis
          dataKey="date"
          tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }}
          tickLine={false}
          axisLine={false}
          interval={2}
        />
        <YAxis
          tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }}
          tickLine={false}
          axisLine={false}
        />
        <Tooltip content={<CustomTooltip />} />
        <Area
          type="monotone"
          dataKey="completed"
          stroke="var(--primary)"
          strokeWidth={2}
          fill="url(#completedGrad)"
          dot={false}
          activeDot={{ r: 4, fill: 'var(--primary)', strokeWidth: 0 }}
        />
        <Area
          type="monotone"
          dataKey="created"
          stroke="#7C3AED"
          strokeWidth={2}
          fill="url(#createdGrad)"
          dot={false}
          activeDot={{ r: 4, fill: '#7C3AED', strokeWidth: 0 }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}