import { BaseAPIClient } from './BaseAPIClient';
import { ENDPOINTS } from '@/config/constants';

export class PortfolioAPIClient extends BaseAPIClient {
  static instance = null;

  static getInstance() {
    if (!PortfolioAPIClient.instance) {
      PortfolioAPIClient.instance = new PortfolioAPIClient();
    }
    return PortfolioAPIClient.instance;
  }

  constructor() {
    super();
  }

  // 포트폴리오 요약 정보 조회
  async getPortfolioSummary() {
    return this.get(ENDPOINTS.PORTFOLIO.SUMMARY);
  }
} 