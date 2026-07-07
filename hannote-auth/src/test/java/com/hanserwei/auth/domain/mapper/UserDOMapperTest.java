package com.hanserwei.auth.domain.mapper;

import com.hanserwei.auth.domain.dataobject.UserDO;
import com.hanserwei.framework.common.enums.DeletedEnum;
import com.hanserwei.framework.common.enums.StatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UserDOMapperTest {

    @Autowired
    private UserDOMapper userDOMapper;

    @Test
    void crud_smoke() {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 11);

        UserDO user = UserDO.builder()
                .phone(unique)
                .hannoteId("HN-" + unique.substring(0, 6))
                .nickname("小憨薯" + unique.substring(0, 6))
                .status(StatusEnum.ENABLE.getValue())
                .deleted(DeletedEnum.NO.getValue())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        int inserted = userDOMapper.insert(user);
        assertEquals(1, inserted);
        assertNotNull(user.getId(), "IDENTITY 主键应回填");

        UserDO found = userDOMapper.selectById(user.getId());
        assertNotNull(found);
        assertEquals(unique, found.getPhone());

        int deleted = userDOMapper.deleteById(user.getId());
        assertTrue(deleted >= 1);
    }
}
