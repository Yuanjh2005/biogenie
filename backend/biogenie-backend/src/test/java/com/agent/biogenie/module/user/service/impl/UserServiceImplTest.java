package com.agent.biogenie.module.user.service.impl;

import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.dto.user.CreateAdminDTO;
import com.agent.biogenie.module.auth.service.UserSessionService;
import com.agent.biogenie.module.admin.service.AdminAuditService;
import com.agent.biogenie.dto.user.UserInfoDTO;
import com.agent.biogenie.dto.user.AdminUserDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.agent.biogenie.module.user.entity.User;
import com.agent.biogenie.module.user.mapper.UserMapper;
import com.agent.biogenie.utils.MinioUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private MinioUtil minioUtil;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void updateAvatar_shouldUploadToMinioAndUpdateUser() {
        User user = new User();
        user.setId(1L);
        user.setAvatar("https://old.example.com/old.png");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        String permanent = "http://127.0.0.1:9000/biogenie/avatars/1/new.png";
        String access = permanent + "?X-Amz-Signature=signed";
        when(userMapper.selectById(1L)).thenReturn(user);
        when(minioUtil.upload(eq(file), eq("avatars/1/"))).thenReturn(permanent);
        when(minioUtil.toAccessUrl(permanent)).thenReturn(access);

        String url = userService.updateAvatar(1L, file);

        assertEquals(access, url);
        assertEquals(permanent, user.getAvatar());
        verify(userMapper).updateById(user);
    }

    @Test
    void updateAvatar_whenEmptyFile_shouldThrow() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        MockMultipartFile empty = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        BizException ex = assertThrows(BizException.class, () -> userService.updateAvatar(1L, empty));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(minioUtil, never()).upload(any(), anyString());
    }

    @Test
    void updateAvatar_whenUnsupportedType_shouldThrow() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.txt",
                "text/plain",
                new byte[]{1, 2, 3}
        );

        BizException ex = assertThrows(BizException.class, () -> userService.updateAvatar(1L, file));
        assertEquals(ResultCode.FILE_TYPE_NOT_SUPPORTED.getCode(), ex.getCode());
        verify(minioUtil, never()).upload(any(), anyString());
    }

    @Test
    void updateStatus_whenDisable_shouldKickAllSessions() {
        User operator = new User();
        operator.setId(99L);
        operator.setRole("SUPER_ADMIN");
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        user.setRole("USER");
        when(userMapper.selectById(99L)).thenReturn(operator);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.updateStatus(99L, 1L, 0);

        assertEquals(0, user.getStatus());
        verify(userMapper).updateById(user);
        verify(userSessionService).removeAllSessions(1L);
    }

    @Test
    void updateStatus_whenEnable_shouldNotKickSessions() {
        User operator = new User();
        operator.setId(99L);
        operator.setRole("ADMIN");
        User user = new User();
        user.setId(1L);
        user.setStatus(0);
        user.setRole("USER");
        when(userMapper.selectById(99L)).thenReturn(operator);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.updateStatus(99L, 1L, 1);

        assertEquals(1, user.getStatus());
        verify(userMapper).updateById(user);
        verify(userSessionService, never()).removeAllSessions(anyLong());
    }

    @Test
    void updateStatus_whenAdminDisablesAdmin_shouldForbid() {
        User operator = new User();
        operator.setId(2L);
        operator.setRole("ADMIN");
        User target = new User();
        target.setId(3L);
        target.setRole("ADMIN");
        when(userMapper.selectById(2L)).thenReturn(operator);
        when(userMapper.selectById(3L)).thenReturn(target);

        BizException ex = assertThrows(BizException.class, () -> userService.updateStatus(2L, 3L, 0));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        verify(userSessionService, never()).removeAllSessions(anyLong());
    }

    @Test
    void createAdmin_whenNewPhone_shouldInsertAdmin() {
        User operator = new User();
        operator.setId(99L);
        operator.setRole("SUPER_ADMIN");
        when(userMapper.selectById(99L)).thenReturn(operator);
        when(userMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        User inserted = new User();
        inserted.setId(10L);
        inserted.setPhone("13800138000");
        inserted.setNickname("管理员8000");
        inserted.setRole("ADMIN");
        when(userMapper.selectById(10L)).thenReturn(inserted);

        CreateAdminDTO dto = new CreateAdminDTO();
        dto.setPhone("13800138000");
        UserInfoDTO result = userService.createAdmin(99L, dto);

        assertEquals(10L, result.getId());
        assertEquals("ADMIN", result.getRole());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void page_whenAvatarServiceUnavailable_shouldStillReturnUsers() {
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setRole("USER");
        user.setAvatar("avatars/1/broken.png");
        Page<User> page = new Page<>(1, 10, 1);
        page.setRecords(java.util.List.of(user));
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(minioUtil.toAccessUrl(user.getAvatar())).thenThrow(new BizException(ResultCode.FILE_UPLOAD_FAILED));

        Page<AdminUserDTO> result = userService.page(1, 10, null);

        assertEquals(1, result.getRecords().size());
        assertNull(result.getRecords().get(0).getAvatar());
        assertEquals("138****8000", result.getRecords().get(0).getPhone());
    }
}
