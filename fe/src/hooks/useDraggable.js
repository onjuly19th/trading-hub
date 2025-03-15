import { useState, useEffect, useRef, useCallback } from 'react';

export function useDraggable(initialPosition, constraints = {}) {
  const [position, setPosition] = useState(initialPosition);
  const isDragging = useRef(false);
  const dragStart = useRef({ x: 0, y: 0 });
  
  const handleMouseDown = useCallback((e) => {
    // 입력 요소나 버튼에서는 드래그 시작하지 않음
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'BUTTON') {
      return;
    }
    
    isDragging.current = true;
    dragStart.current = { x: e.clientX, y: e.clientY };
  }, []);

  useEffect(() => {
    const handleMouseMove = (e) => {
      if (!isDragging.current) return;

      const dx = e.clientX - dragStart.current.x;
      const dy = e.clientY - dragStart.current.y;

      setPosition(prev => {
        const newX = prev.x + dx;
        const newY = prev.y + dy;

        // 화면 경계 제한
        const minX = constraints.minX ?? 0;
        const maxX = constraints.maxX ?? window.innerWidth - 340;
        const minY = constraints.minY ?? 0;
        const maxY = constraints.maxY ?? window.innerHeight - 100;

        return {
          x: Math.min(Math.max(newX, minX), maxX),
          y: Math.min(Math.max(newY, minY), maxY)
        };
      });

      dragStart.current = { x: e.clientX, y: e.clientY };
    };

    const handleMouseUp = () => {
      isDragging.current = false;
    };

    // 윈도우 리사이즈 시 위치 조정
    const handleResize = () => {
      setPosition(prev => ({
        x: Math.min(Math.max(prev.x, 0), window.innerWidth - 340),
        y: Math.min(Math.max(prev.y, 0), window.innerHeight - 100)
      }));
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
    window.addEventListener('resize', handleResize);

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      window.removeEventListener('resize', handleResize);
    };
  }, [constraints]);

  return {
    position,
    handleMouseDown,
    isDragging: isDragging.current
  };
} 