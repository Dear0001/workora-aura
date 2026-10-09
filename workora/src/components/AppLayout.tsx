'use client';
import React, { useState } from 'react';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

interface Props {
  children: React.ReactNode;
}

export default function AppLayout({ children }: Props) {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background flex">
      {/* Mobile overlay */}
      {mobileSidebarOpen && (
        <div
          className="fixed inset-0 bg-black/40 z-30 lg:hidden"
          onClick={() => setMobileSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <Sidebar
        collapsed={sidebarCollapsed}
        mobileOpen={mobileSidebarOpen}
        onMobileClose={() => setMobileSidebarOpen(false)}
      />

      {/* Main content */}
      <div
        className="flex-1 flex flex-col min-w-0 content-transition"
        style={{
          marginLeft: sidebarCollapsed ? '64px' : '240px',
        }}
      >
        <Topbar
          onToggleSidebar={() => setSidebarCollapsed(!sidebarCollapsed)}
          onMobileMenuOpen={() => setMobileSidebarOpen(true)}
          sidebarCollapsed={sidebarCollapsed}
        />
        <main className="flex-1 p-6 lg:p-8 overflow-auto">
          <div className="max-w-screen-2xl mx-auto">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}