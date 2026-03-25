package com.bancup;

import com.bancup.repository.AuditoriaEventoRepository;
import com.bancup.repository.CatEstadoRepository;
import com.bancup.repository.CatGeneroRepository;
import com.bancup.repository.CodigoVerificacionCorreoRepository;
import com.bancup.repository.CuentaRepository;
import com.bancup.repository.DispositivoRepository;
import com.bancup.repository.LoginAuditRepository;
import com.bancup.repository.RefreshTokenRepository;
import com.bancup.repository.RolRepository;
import com.bancup.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "jwt.secret=test-secret",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
class BancupApplicationTests {

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private RolRepository rolRepository;

    @MockBean
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @MockBean
    private CatGeneroRepository catGeneroRepository;

    @MockBean
    private CodigoVerificacionCorreoRepository codigoVerificacionCorreoRepository;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private CatEstadoRepository catEstadoRepository;

    @MockBean
    private LoginAuditRepository loginAuditRepository;

    @MockBean
    private CuentaRepository cuentaRepository;

    @MockBean
    private DispositivoRepository dispositivoRepository;

    @Test
    void contextLoads() {
    }

}
