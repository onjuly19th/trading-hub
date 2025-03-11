import { useState } from "react";
import axios from "axios";

function PriceFetcher() {
    const [symbol, setSymbol] = useState("bitcoin"); // 기본값: bitcoin
    const [type, setType] = useState("crypto"); // 기본값: crypto
    const [price, setPrice] = useState(null);
    const [error, setError] = useState(null);

    // API 호출 함수
    const fetchPrice = async () => {
        try {
            setError(null); // 기존 에러 초기화
            const response = await axios.get(`http://localhost:8080/api/${type}?symbol=${symbol}`);
            setPrice(response.data); // 가격 업데이트
        } catch (err) {
            setError("가격을 가져오는 중 오류가 발생했습니다.");
            console.error(err);
        }
    };

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-100">
            <div className="p-8 bg-white shadow-lg rounded-lg w-full max-w-md">
                <h2 className="text-2xl font-semibold mb-4">투자 상품 가격 조회</h2>

                {/* 티커 입력 */}
                <input 
                    type="text" 
                    value={symbol} 
                    onChange={(e) => setSymbol(e.target.value)} 
                    placeholder="티커 입력 (예: bitcoin, AAPL)"
                    className="w-full p-2 border border-gray-300 rounded-md mb-4"
                />

                {/* 조회 타입 선택 */}
                <select 
                    value={type} 
                    onChange={(e) => setType(e.target.value)} 
                    className="w-full p-2 border border-gray-300 rounded-md mb-4"
                >
                    <option value="crypto">암호화폐</option>
                    <option value="stock">주식</option>
                </select>

                {/* 가격 조회 버튼 */}
                <button 
                    onClick={fetchPrice} 
                    className="w-full p-2 bg-blue-500 text-white rounded-md mb-4"
                >
                    가격 조회
                </button>

                {/* 결과 표시 */}
                {error && <p className="text-red-500">{error}</p>}
                {price && (
                    <div>
                      <div className="tw-px-4 tw-py-2 tw-bg-white dark:tw-bg-gray-800 tw-rounded-md">
                        <div className="tw-flex tw-items-center tw-w-full">
                          {/* 이미지 */}
                          {price.image && (
                            <img
                              src={price.image}
                              alt={symbol}
                              className="tw-mr-2 !tw-h-6 tw-w-6 tw-object-fill"
                              loading="lazy"
                            />
                          )}

                          {/* 텍스트 내용 */}
                          <div className="tw-flex tw-flex-col 2lg:tw-flex-row tw-items-start 2lg:tw-items-center">
                            <div className="tw-text-gray-700 dark:tw-text-moon-100 tw-font-semibold tw-text-sm tw-leading-5">
                              {symbol.toUpperCase()}
                              {/* 하위 텍스트 (예: BTC) */}
                              <div className="tw-block 2lg:tw-inline tw-text-xs tw-leading-4 tw-text-gray-500 dark:tw-text-moon-200 tw-font-medium">
                                {symbol.toUpperCase()}
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                        <p>USD: ${price.USD}</p>
                        <p>KRW: ₩{price.KRW}</p>
                    </div>
                )}
            </div>
        </div>
    );
}

export default PriceFetcher;
