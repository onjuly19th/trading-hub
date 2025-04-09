import React from 'react';

const AuthTitle = ({ children }) => {
  return (
    <div>
      <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
        {children}
      </h2>
    </div>
  );
};

export default AuthTitle; 