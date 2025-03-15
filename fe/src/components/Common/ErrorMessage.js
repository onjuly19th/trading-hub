export default function ErrorMessage({ message }) {
  if (!message) return null;
  
  return (
    <div className="absolute inset-0 flex items-center justify-center bg-black/10 z-20">
      <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
        {message}
      </div>
    </div>
  );
} 