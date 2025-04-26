import React from 'react';

const AuthInput = ({ 
  id, 
  name,
  label,
  type = "text", 
  placeholder, 
  value, 
  onChange,
  required = false,
  className = "" 
}) => {
  // id와 name이 지정되지 않은 경우 label을 사용
  const inputId = id || name || label?.toLowerCase().replace(/\s+/g, '-');
  const inputName = name || inputId;
  
  return (
    <div className="mb-4">
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-gray-700 mb-1">
          {label}
        </label>
      )}
      <input
        id={inputId}
        name={inputName}
        type={type}
        required={required}
        className={`appearance-none rounded-md relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm ${className}`}
        placeholder={placeholder || label}
        value={value}
        onChange={onChange}
      />
    </div>
  );
};

export default AuthInput; 