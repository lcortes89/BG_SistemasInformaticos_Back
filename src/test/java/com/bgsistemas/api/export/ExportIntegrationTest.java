package com.bgsistemas.api.export;

import com.bgsistemas.api.client.Client;
import com.bgsistemas.api.client.ClientRepository;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-255: whole application + H2. The admin downloads the clients and the Excel has them all
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    @Test
    void theExcelHasEveryClientEvenTheHiddenOnes() throws Exception {
        clientRepository.save(new Client(null, "Zeta Export Bar", "Bar", "Muy bien", null, 5, true));
        clientRepository.save(new Client(null, "Zeta Export Hotel", "Hotel", "Genial", null, 4, false));

        byte[] file = mockMvc.perform(get("/api/admin/clients/export").param("format", "xlsx").with(user(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            Sheet sheet = workbook.getSheet("Clientes");
            List<String> names = new ArrayList<>();
            for (Row row : sheet) {
                names.add(row.getCell(0).getStringCellValue());
            }
            assertThat(names.get(0)).isEqualTo("Nombre");
            assertThat(names).contains("Zeta Export Bar", "Zeta Export Hotel");
        }
    }
}
