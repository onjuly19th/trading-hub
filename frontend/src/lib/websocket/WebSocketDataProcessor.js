export const processWebSocketData = (streamType, data) => {
  try {    
    switch (streamType) {
      case 'ticker':
        return {
          price: parseFloat(data.c),
          priceChange: parseFloat(data.p),
          priceChangePercent: parseFloat(data.P),
          volume: parseFloat(data.v),
          quoteVolume: parseFloat(data.q),
          lastQty: parseFloat(data.Q),
          bestBid: parseFloat(data.b),
          bestAsk: parseFloat(data.a),
          highPrice: parseFloat(data.h),
          lowPrice: parseFloat(data.l),
          openPrice: parseFloat(data.o),
          closePrice: parseFloat(data.c),
          type: 'ticker'
        };
      case 'trade':
        return {
          price: parseFloat(data.p),
          amount: parseFloat(data.q),
          time: data.T,
          isBuyerMaker: data.m,
          type: 'trade'
        };
      case 'depth20':
        return {
          bids: data.bids.map(([price, quantity]) => ({
            price: parseFloat(price),
            quantity: parseFloat(quantity)
          })),
          asks: data.asks.map(([price, quantity]) => ({
            price: parseFloat(price),
            quantity: parseFloat(quantity)
          })),
          type: 'depth'
        };
      default:
        console.warn(`Unknown stream type: ${streamType}`);
        return data;
    }
  } catch (error) {
    console.error(`Error processing ${streamType} data:`, error);
    return null;
  }
};
  