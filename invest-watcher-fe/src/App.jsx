import { useState } from "react";
import axios from "axios";

function PriceFetcher() {
    const [symbol, setSymbol] = useState("bitcoin"); // 기본값: 비트코인
    const [type, setType] = useState("crypto"); // 기본값: 암호화폐
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
        <div>
            <h2>투자 상품 가격 조회</h2>

            {/* 심볼 입력 */}
            <input 
                type="text" 
                value={symbol} 
                onChange={(e) => setSymbol(e.target.value)} 
                placeholder="심볼 입력 (예: bitcoin, AAPL)"
            />

            {/* 조회 타입 선택 */}
            <select value={type} onChange={(e) => setType(e.target.value)}>
                <option value="crypto">암호화폐</option>
                <option value="stock">주식</option>
            </select>

            {/* 가격 조회 버튼 */}
            <button onClick={fetchPrice}>가격 조회</button>

            {/* 결과 표시 */}
            {error && <p style={{ color: "red" }}>{error}</p>}
            {price && (
                <div>
                    <h3>{symbol.toUpperCase()} 가격</h3>
                    <p>USD: ${price.USD}</p>
                    <p>KRW: ₩{price.KRW}</p>
                </div>
            )}
        </div>
    );
}

export default PriceFetcher;