import React from 'react';

const StatusMessage = ({ error, success }) => {
  if (error) {
    return <div className="text-red-500 text-sm text-center">{error}</div>;
  }
  if (success) {
    return <div className="text-green-500 text-sm text-center">{success}</div>;
  }
  return null;
};

export default StatusMessage; 