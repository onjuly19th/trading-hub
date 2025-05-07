package com.tradinghub.interfaces.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradinghub.application.service.order.OrderApplicationService;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.Order.OrderSide;
import com.tradinghub.domain.model.order.Order.OrderStatus;
import com.tradinghub.domain.model.order.Order.OrderType;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.repository.UserRepository;
import com.tradinghub.infrastructure.security.CustomUserDetailsService;
import com.tradinghub.infrastructure.security.JwtAuthenticationFilter;
import com.tradinghub.infrastructure.security.JwtService;
import com.tradinghub.infrastructure.security.SecurityConfig;
import com.tradinghub.interfaces.dto.order.OrderRequest;
import com.tradinghub.interfaces.dto.order.OrderResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CustomUserDetailsService.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderApplicationService orderService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    private User testUser;
    private Order testOrder;
    private OrderRequest testOrderRequest;
    private OrderResponse testOrderResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setPassword("password");

        given(userRepository.findByUsername("testUser"))
                .willReturn(java.util.Optional.of(testUser));

        testOrder = Order.builder()
                .user(testUser)
                .symbol("BTCUSDT")
                .type(OrderType.MARKET)
                .side(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .amount(new BigDecimal("0.1"))
                .status(OrderStatus.FILLED)
                .build();

        // Set ID using reflection
        ReflectionTestUtils.setField(testOrder, "id", 1L);

        testOrderRequest = new OrderRequest(
                "BTCUSDT", 
                OrderType.MARKET, 
                OrderSide.BUY, 
                new BigDecimal("50000.00"), 
                new BigDecimal("0.1")
        );

        testOrderResponse = OrderResponse.from(testOrder);
    }

    @Test
    @DisplayName("시장가 주문 생성 성공")
    @WithMockUser(username = "testUser")
    void createOrder_Success_MarketOrder() throws Exception {
        // GIVEN
        given(orderService.createMarketOrder(anyLong(), anyString(), any(OrderSide.class), 
                any(BigDecimal.class), any(BigDecimal.class)))
                .willReturn(testOrder);

        // WHEN & THEN
        ResultActions result = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testOrderRequest)));

        // Log the response for debugging
        String responseContent = result.andReturn().getResponse().getContentAsString();
        log.error("Response content: {}", responseContent);

        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testOrderResponse.id()))
                .andExpect(jsonPath("$.symbol").value(testOrderResponse.symbol()))
                .andExpect(jsonPath("$.type").value(testOrderResponse.type()))
                .andExpect(jsonPath("$.side").value(testOrderResponse.side()))
                .andExpect(jsonPath("$.price").value(testOrderResponse.price().doubleValue()))
                .andExpect(jsonPath("$.amount").value(testOrderResponse.amount().doubleValue()))
                .andExpect(jsonPath("$.status").value(testOrderResponse.status()));
    }

    @Test
    @DisplayName("지정가 주문 생성 성공")
    @WithMockUser(username = "testUser")
    void createOrder_Success_LimitOrder() throws Exception {
        // GIVEN
        testOrderRequest = new OrderRequest(    
                "BTCUSDT", 
                OrderType.LIMIT, 
                OrderSide.BUY, 
                new BigDecimal("50000.00"), 
                new BigDecimal("0.1")
        );
        
        Order limitOrder = Order.builder()
                .user(testUser)
                .symbol("BTCUSDT")
                .type(OrderType.LIMIT)
                .side(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .amount(new BigDecimal("0.1"))
                .status(OrderStatus.PENDING)
                .build();

        // Set ID using reflection
        ReflectionTestUtils.setField(limitOrder, "id", 1L);

        given(orderService.createLimitOrder(anyLong(), anyString(), any(OrderSide.class), 
                any(BigDecimal.class), any(BigDecimal.class)))
                .willReturn(limitOrder);

        // WHEN & THEN
        ResultActions result = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testOrderRequest)));

        // Log the response for debugging
        String responseContent = result.andReturn().getResponse().getContentAsString();
        log.error("Response content: {}", responseContent);

        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.type").value("LIMIT"))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.price").value(50000.0))
                .andExpect(jsonPath("$.amount").value(0.1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("사용자의 미체결 주문 조회 성공")
    @WithMockUser(username = "testUser")
    void getPendingOrders_Success() throws Exception {
        // GIVEN
        // 미체결 상태의 주문 생성
        Order pendingOrder = Order.builder()
                .user(testUser)
                .symbol("ETHUSDT")
                .type(OrderType.LIMIT)
                .side(OrderSide.SELL)
                .price(new BigDecimal("3000.00"))
                .amount(new BigDecimal("0.5"))
                .status(OrderStatus.PENDING) // 미체결 상태
                .build();
        ReflectionTestUtils.setField(pendingOrder, "id", 2L); // 고유 ID 설정

        List<Order> pendingOrders = Collections.singletonList(pendingOrder);
        List<OrderResponse> orderResponses = OrderResponse.fromList(pendingOrders);

        // Mock 설정: getPendingOrdersByUserId 사용
        given(orderService.getPendingOrdersByUserId(testUser.getId()))
                .willReturn(pendingOrders);

        // WHEN & THEN
        ResultActions result = mockMvc.perform(get("/api/orders"));

        // Log the response for debugging
        String responseContent = result.andReturn().getResponse().getContentAsString();
        log.error("Response content for getPendingOrders_Success: {}", responseContent);

        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(orderResponses.get(0).id()))
                .andExpect(jsonPath("$[0].symbol").value(orderResponses.get(0).symbol()))
                .andExpect(jsonPath("$[0].type").value(orderResponses.get(0).type()))
                .andExpect(jsonPath("$[0].side").value(orderResponses.get(0).side()))
                .andExpect(jsonPath("$[0].price").value(orderResponses.get(0).price().doubleValue()))
                .andExpect(jsonPath("$[0].amount").value(orderResponses.get(0).amount().doubleValue()))
                .andExpect(jsonPath("$[0].status").value(orderResponses.get(0).status()));
    }

    @Test
    @DisplayName("사용자의 거래 내역 조회 성공")
    @WithMockUser(username = "testUser")
    void getOrderHistory_Success() throws Exception {
        // GIVEN
        List<Order> completedOrders = Collections.singletonList(testOrder);
        //List<OrderResponse> orderResponses = OrderResponse.fromList(completedOrders);

        given(orderService.getCompletedOrdersByUserId(anyLong()))
                .willReturn(completedOrders);

        // WHEN & THEN
        ResultActions result = mockMvc.perform(get("/api/orders/history"));

        // Log the response for debugging
        String responseContent = result.andReturn().getResponse().getContentAsString();
        log.error("Response content: {}", responseContent);

        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$[0].type").value("MARKET"))
                .andExpect(jsonPath("$[0].side").value("BUY"))
                .andExpect(jsonPath("$[0].price").value(50000.0))
                .andExpect(jsonPath("$[0].amount").value(0.1))
                .andExpect(jsonPath("$[0].status").value("FILLED"));
    }

    @Test
    @DisplayName("특정 심볼의 주문 조회 성공")
    @WithMockUser(username = "testUser")
    void getOrdersBySymbol_Success() throws Exception {
        // GIVEN
        List<Order> orders = Collections.singletonList(testOrder);
        //List<OrderResponse> orderResponses = OrderResponse.fromList(orders);

        given(orderService.getOrdersByUserIdAndSymbol(anyLong(), anyString()))
                .willReturn(orders);

        // WHEN & THEN
        try {
            // 슬래시 대신 하이픈을 사용하여 심볼을 표현
            String symbol = "BTCUSDT";
            ResultActions result = mockMvc.perform(get("/api/orders/symbol/{symbol}", symbol));

            // Log the response for debugging
            String responseContent = result.andReturn().getResponse().getContentAsString();
            log.error("Response content: {}", responseContent);

            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].symbol").value("BTCUSDT"))
                    .andExpect(jsonPath("$[0].type").value("MARKET"))
                    .andExpect(jsonPath("$[0].side").value("BUY"))
                    .andExpect(jsonPath("$[0].price").value(50000.0))
                    .andExpect(jsonPath("$[0].amount").value(0.1))
                    .andExpect(jsonPath("$[0].status").value("FILLED"));
        } catch (Exception e) {
            log.error("Test failed with exception", e);
            throw e;
        }
    }

    @Test
    @DisplayName("주문 취소 성공")
    @WithMockUser(username = "testUser")
    void cancelOrder_Success() throws Exception {
        // GIVEN
        Long orderId = 1L;

        // WHEN & THEN
        ResultActions result = mockMvc.perform(delete("/api/orders/{orderId}", orderId));

        // Log the response for debugging
        String responseContent = result.andReturn().getResponse().getContentAsString();
        log.error("Response content: {}", responseContent);

        result.andExpect(status().isOk());

        then(orderService).should(times(1)).cancelOrder(orderId, testUser.getId());
    }
} 