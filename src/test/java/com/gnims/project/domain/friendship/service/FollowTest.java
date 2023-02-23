//package com.gnims.project.domain.friendship.service;
//
//import com.gnims.project.domain.friendship.repository.FriendshipRepository;
//import com.gnims.project.domain.user.entity.User;
//import com.gnims.project.domain.user.repository.UserRepository;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.TransactionStatus;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.transaction.support.DefaultTransactionDefinition;
//
//import static com.gnims.project.domain.friendship.entity.FollowStatus.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * 팔로우 하기/취소 API 테스트
// */
//@AutoConfigureMockMvc
//@SpringBootTest
//class FollowTest {
//
//    @Autowired
//    MockMvc mvc;
//
//    @Autowired
//    UserRepository userRepository;
//
//    @Autowired
//    FriendshipRepository friendshipRepository;
//
//    String authorization = null;
//
//    @Autowired
//    PlatformTransactionManager transactionManager;
//
//    TransactionStatus status = null;
//
//    @BeforeEach
//    void beforeEach() throws Exception {
//        MockMultipartFile file1 = new MockMultipartFile(
//                "data", "", "application/json",
//                "{\"nickname\" : \"딸기\",\"username\": \"이땡땡\", \"email\": \"ddalgi@gmail.com\", \"password\": \"123456aA9\"}".getBytes());
//        mvc.perform(multipart("/auth/signup")
//                .file(file1));
//
//        MockMultipartFile file2 = new MockMultipartFile(
//                "data", "", "application/json",
//                "{\"nickname\" : \"당근\",\"username\": \"김땡땡\", \"email\": \"danguen@gmail.com\", \"password\": \"123456aA9\"}".getBytes());
//        mvc.perform(multipart("/auth/signup")
//                .file(file2));
//
//        MockMultipartFile file3 = new MockMultipartFile(
//                "data", "", "application/json",
//                "{\"nickname\" : \"수박\",\"username\": \"박땡땡\", \"email\": \"suback@gmail.com\", \"password\": \"123456aA9\"}".getBytes());
//        mvc.perform(multipart("/auth/signup")
//                .file(file3));
//
//        MockMultipartFile file4 = new MockMultipartFile(
//                "data", "", "application/json",
//                "{\"nickname\" : \"참외\",\"username\": \"최땡땡\", \"email\": \"chamwhe@gmail.com\", \"password\": \"123456aA9\"}".getBytes());
//        mvc.perform(multipart("/auth/signup")
//                .file(file4));
//
//        MvcResult result = mvc.perform(post("/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content("{\"email\": \"ddalgi@gmail.com\", \"password\": \"123456aA9\"}")).andReturn();
//
//        authorization = result.getResponse().getHeader("Authorization");
//    }
//
//    @AfterEach
//    void afterEach() {
//        friendshipRepository.deleteAll();
//        userRepository.deleteAll();
//    }
//
//    @Transactional
//    @DisplayName("최초 팔로우 시 - 상태 코드 201, status 필드 'INIT' 반환")
//    @Test
//    void test1() throws Exception {
//        status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//        transactionManager.commit(status);
//        //given
//        String expression = "$.data[?(@.status == '%s')]";
//
//        User user = userRepository.findByNickname("수박").get();
//        Long userId = user.getId();
//
//        //when
//        mvc.perform(post("/friendship/followings/"+ userId)
//                .header("Authorization", authorization))
//                //then
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath(expression, INIT).exists());
//
//    }
//
//    @DisplayName("팔로우 취소 - 상태 코드 200, status 필드 'INACTIVE' 반환")
//    @Test
//    void test2() throws Exception {
//        status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//        transactionManager.commit(status);
//        //given
//        String expression = "$.data[?(@.status == '%s')]";
//
//        User user = userRepository.findByNickname("수박").get();
//        Long userId = user.getId();
//
//        // 최초 팔로우
//        mvc.perform(post("/friendship/followings/"+ userId)
//                .header("Authorization", authorization));
//
//        //when : 팔로우 취소
//        mvc.perform(post("/friendship/followings/"+ userId)
//                        .header("Authorization", authorization))
//                //then
//                .andExpect(status().isOk())
//                .andExpect(jsonPath(expression, INACTIVE).exists());
//    }
//
//    @DisplayName("팔로우 다시 하기 - 상태 코드 200, status 필드 'ACTIVE' 반환")
//    @Test
//    void test3() throws Exception {
//        status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//        //given
//        transactionManager.commit(status);
//        String expression = "$.data[?(@.status == '%s')]";
//
//        User user = userRepository.findByNickname("수박").get();
//        Long userId = user.getId();
//
//        // 최초 팔로우
//        mvc.perform(post("/friendship/followings/"+ userId)
//                .header("Authorization", authorization));
//
//        // 팔로우 취소
//        mvc.perform(post("/friendship/followings/"+ userId)
//                .header("Authorization", authorization));
//
//        //when : 팔로우 다시 맺기
//        mvc.perform(post("/friendship/followings/"+ userId)
//                .header("Authorization", authorization))
//                //then
//                .andExpect(status().isOk())
//                .andExpect(jsonPath(expression, ACTIVE).exists());
//    }
//}