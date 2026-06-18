'use client';

import { Bell, Search, Menu } from 'lucide-react';

export function Topbar({ onMenuClick }: { onMenuClick?: () => void }) {
  return (
    <header className="sticky top-0 z-40 flex h-16 shrink-0 items-center gap-x-4 border-b border-[var(--color-border)] bg-[var(--color-background)]/80 backdrop-blur-md px-4 sm:gap-x-6 sm:px-6 lg:px-8">
      {/* Mobile menu button - only visible below lg */}
      {onMenuClick && (
        <button
          onClick={onMenuClick}
          className="lg:hidden -ml-1 p-2 rounded-lg text-[var(--color-primary-muted)] hover:text-white hover:bg-[var(--color-surface-hover)] transition-colors"
          aria-label="Abrir menu"
        >
          <Menu className="h-5 w-5" />
        </button>
      )}

      <div className="flex flex-1 gap-x-4 self-stretch lg:gap-x-6">
        <form className="relative flex flex-1" onSubmit={(e) => e.preventDefault()}>
          <label htmlFor="search-field" className="sr-only">
            Buscar...
          </label>
          <Search
            className="pointer-events-none absolute inset-y-0 left-0 h-full w-4 text-[var(--color-primary-muted)]"
            aria-hidden="true"
          />
          <input
            id="search-field"
            className="block h-full w-full border-0 bg-transparent py-0 pl-8 pr-0 text-white focus:ring-0 sm:text-sm placeholder:text-[var(--color-primary-muted)] outline-none"
            placeholder="Buscar dispositivos, feedbacks, configs..."
            type="search"
            name="search"
          />
        </form>
        <div className="flex items-center gap-x-4 lg:gap-x-6">
          <button type="button" className="-m-2.5 p-2.5 text-[var(--color-primary-muted)] hover:text-white transition-colors relative">
            <span className="sr-only">Notificações</span>
            <Bell className="h-5 w-5" aria-hidden="true" />
            <span className="absolute top-2 right-2 h-2 w-2 rounded-full bg-red-500 ring-2 ring-[var(--color-background)]" />
          </button>
          
          <div className="hidden lg:block lg:h-6 lg:w-px lg:bg-[var(--color-border)]" aria-hidden="true" />
          
          {/* Status Indicator */}
          <div className="flex items-center gap-2 text-sm text-[var(--color-primary-muted)]">
            <span className="h-2 w-2 rounded-full bg-green-500 animate-pulse" />
            <span className="hidden sm:inline">API Online</span>
          </div>
        </div>
      </div>
    </header>
  );
}
