
package com.msbillingauthapi.controller;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



import java.time.LocalDate;
import java.util.List;
import java.util.Map;


import com.msbillingauthapi.dto.RegistrationRequestDto;
import com.msbillingauthapi.entity.User;
import com.msbillingauthapi.exception.ProblemDetailBuilder;
import com.msbillingauthapi.exception.RestErrorResponseException;
import com.msbillingauthapi.mapper.UserRegistrationMapper;
import com.msbillingauthapi.service.EmailVerificationService;
import com.msbillingauthapi.service.UserRegistrationService;
import com.msbillingauthapi.testdata.TestUserBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@WebMvcTest(RegistrationController.class)
@Import(UserRegistrationMapper.class)
class RegistrationControllerTest extends ControllerTest {

    private static final String USERNAME = "testUser";
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password123";
    private static final String LAST_NAME = "Doe";
    private static final String FIRST_NAME = "John";
    private static final String PHONE_NUMBER = "1234567890";
    private static final String ADDRESS = "123 Street";
    private static final String CITY = "SampleCity";
    private static final Integer ROLE = 1;
    private static final String POSTAL_CODE = "12345";
    private static final String GENDER = "M";
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);
    private static final String SALUTATION = "Mr";

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private UserRegistrationService userRegistrationService;

    @Test
    void registerUser_validRequest_returnsOk() throws Exception {
        final var user = TestUserBuilder.userBuilder().build();

        when(userRegistrationService.registerUser(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequestDto(USERNAME, EMAIL, PASSWORD, LAST_NAME, FIRST_NAME,
                                        PHONE_NUMBER, ADDRESS, CITY, ROLE))))
                .andExpect(status().isOk());

        verify(userRegistrationService).registerUser(any());
        verify(emailVerificationService).sendEmailVerificationOtp(eq(user.getId()), eq(user.getEmail()));
    }

    @Test
    void registerUser_emailOrUsernameExists_returnsConflict() throws Exception {
        final var errors = Map.of("email", List.of("Email is already taken"));
        final var restErrorResponseException = new RestErrorResponseException(ProblemDetailBuilder.forStatusAndDetail(CONFLICT, "Request validation failed").withProperty("errors", errors).build());

        doThrow(restErrorResponseException)
                .when(userRegistrationService)
                .registerUser(any());

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequestDto(USERNAME, EMAIL, PASSWORD, LAST_NAME, FIRST_NAME,
                                        PHONE_NUMBER, ADDRESS, CITY, ROLE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Request validation failed"));

        verify(userRegistrationService).registerUser(any());
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void registerUser_invalidEmailFormat_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequestDto(USERNAME, "invalid-email", PASSWORD, LAST_NAME, FIRST_NAME,
                                        PHONE_NUMBER, ADDRESS, CITY, ROLE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Please provide a valid email address"));
    }

    @Test
    void registerUser_invalidUsernameLength_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequestDto("tu", EMAIL, PASSWORD, LAST_NAME, FIRST_NAME,
                                        PHONE_NUMBER, ADDRESS, CITY, ROLE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("Username must be between 3 and 20 characters"));
    }
}

