'use client';

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ReactNode,
  type RefObject,
} from 'react';
import { createPortal } from 'react-dom';

interface TriggerProps {
  ref: RefObject<HTMLButtonElement | null>;
  onClick: () => void;
  'aria-haspopup': 'menu';
  'aria-expanded': boolean;
}

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  trigger: (props: TriggerProps) => ReactNode;
  children: ReactNode;
  align?: 'start' | 'end';
  widthClassName?: string;
  className?: string;
}

interface Position {
  top: number;
  left: number;
  maxHeight: number;
}

const VIEWPORT_GUTTER = 8;
const MENU_GAP = 4;

export default function FloatingMenu({
  open,
  onOpenChange,
  trigger,
  children,
  align = 'end',
  widthClassName = 'w-40',
  className = '',
}: Props) {
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const [portalRoot, setPortalRoot] = useState<HTMLElement | null>(null);
  const [position, setPosition] = useState<Position | null>(null);

  useEffect(() => {
    setPortalRoot(document.body);
  }, []);

  const updatePosition = useCallback(() => {
    const anchor = triggerRef.current;
    const menu = menuRef.current;
    if (!anchor || !menu) return;

    const anchorRect = anchor.getBoundingClientRect();
    const menuRect = menu.getBoundingClientRect();
    const viewportWidth = document.documentElement.clientWidth;
    const viewportHeight = document.documentElement.clientHeight;
    if (
      anchorRect.bottom < 0 ||
      anchorRect.top > viewportHeight ||
      anchorRect.right < 0 ||
      anchorRect.left > viewportWidth
    ) {
      onOpenChange(false);
      return;
    }
    const below = viewportHeight - anchorRect.bottom - MENU_GAP - VIEWPORT_GUTTER;
    const above = anchorRect.top - MENU_GAP - VIEWPORT_GUTTER;
    const placeAbove = below < menuRect.height && above > below;
    const maxHeight = Math.max(0, placeAbove ? above : below);
    const top = placeAbove
      ? Math.max(VIEWPORT_GUTTER, anchorRect.top - Math.min(menuRect.height, maxHeight) - MENU_GAP)
      : Math.min(
          viewportHeight - VIEWPORT_GUTTER - Math.min(menuRect.height, maxHeight),
          anchorRect.bottom + MENU_GAP
        );
    const preferredLeft = align === 'end' ? anchorRect.right - menuRect.width : anchorRect.left;
    const left = Math.min(
      Math.max(VIEWPORT_GUTTER, preferredLeft),
      Math.max(VIEWPORT_GUTTER, viewportWidth - menuRect.width - VIEWPORT_GUTTER)
    );

    setPosition({ top, left, maxHeight });
  }, [align, onOpenChange]);

  useEffect(() => {
    if (!open || !portalRoot) {
      setPosition(null);
      return;
    }

    updatePosition();

    const handlePointerDown = (event: PointerEvent) => {
      const target = event.target;
      if (
        target instanceof Node &&
        !menuRef.current?.contains(target) &&
        !triggerRef.current?.contains(target)
      ) {
        onOpenChange(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onOpenChange(false);
        triggerRef.current?.focus();
      }
    };
    const handleViewportChange = () => updatePosition();

    document.addEventListener('pointerdown', handlePointerDown, true);
    document.addEventListener('keydown', handleKeyDown);
    window.addEventListener('resize', handleViewportChange);
    window.addEventListener('scroll', handleViewportChange, true);

    const observer = menuRef.current ? new ResizeObserver(handleViewportChange) : null;
    if (menuRef.current) observer?.observe(menuRef.current);

    return () => {
      document.removeEventListener('pointerdown', handlePointerDown, true);
      document.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('resize', handleViewportChange);
      window.removeEventListener('scroll', handleViewportChange, true);
      observer?.disconnect();
    };
  }, [onOpenChange, open, portalRoot, updatePosition]);

  return (
    <>
      {trigger({
        ref: triggerRef,
        onClick: () => onOpenChange(!open),
        'aria-haspopup': 'menu',
        'aria-expanded': open,
      })}
      {open && portalRoot
        ? createPortal(
            <div
              ref={menuRef}
              role="menu"
              className={`fixed ${widthClassName} max-w-[calc(100vw-16px)] overflow-y-auto rounded-xl border border-border bg-card py-1 shadow-elevated z-[1000] ${
                position ? 'visible' : 'invisible'
              } ${className}`}
              style={{
                top: position?.top ?? 0,
                left: position?.left ?? 0,
                maxHeight: position ? `${position.maxHeight}px` : undefined,
              }}
              onClick={(event) => {
                event.stopPropagation();
                onOpenChange(false);
              }}
            >
              {children}
            </div>,
            portalRoot
          )
        : null}
    </>
  );
}
