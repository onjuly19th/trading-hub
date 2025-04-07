export default function LoadingSpinner({ size = 'md' }) {
  // 크기에 따른 클래스 설정
  const sizeClasses = {
    'sm': 'h-6 w-6 border-t-1 border-b-1',
    'md': 'h-12 w-12 border-t-2 border-b-2',
    'lg': 'h-16 w-16 border-t-3 border-b-3'
  };

  const spinnerClass = sizeClasses[size] || sizeClasses.md;
  
  return (
    <div className={size === 'sm' ? 'flex items-center justify-center' : 'min-h-screen flex items-center justify-center'}>
      <div className={`animate-spin rounded-full ${spinnerClass} border-black`}></div>
    </div>
  );
} 