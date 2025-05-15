import Image from 'next/image';

const CryptoLabel = ({ logo, name }) => (
  <div className="relative w-6 h-6">
    <Image
      src={logo}
      alt={name}
      width={24}
      height={24}
      className="rounded-full"
    />
  </div>
);

export default CryptoLabel;