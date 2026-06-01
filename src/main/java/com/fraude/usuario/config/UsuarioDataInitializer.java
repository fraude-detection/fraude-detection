package com.fraude.usuario.config;

import com.fraude.cuenta.model.Cuenta;
import com.fraude.cuenta.repository.CuentaRepository;
import com.fraude.rol.model.Rol;
import com.fraude.rol.repository.RolRepository;
import com.fraude.usuario.model.Usuario;
import com.fraude.usuario.model.UsuarioId;
import com.fraude.usuario.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class UsuarioDataInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final RolRepository rolRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearAdmin();
        crearUsuarioNormal();
    }

    private void crearAdmin() {
        String doc = "1000000001";
        if (usuarioRepository.findByNumDocumento(doc).isPresent()) {
            log.info("Admin ya existe, omitiendo creación");
            return;
        }
        Rol rolAdmin = rolRepository.findByNombre("ADMIN")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("ADMIN").build()));

        Usuario admin = Usuario.builder()
                .id(new UsuarioId(doc))
                .nombre("Admin")
                .apellido("Sistema")
                .email("admin@fraude.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .rolId(rolAdmin.getId())
                .estadoId(1)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        usuarioRepository.save(admin);

        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta("ACC-ADMIN-001")
                .saldo(BigDecimal.valueOf(500000))
                .numDocumento(doc)
                .build();
        cuentaRepository.save(cuenta);

        log.info("Admin creado — doc: {} | pass: admin123 | cuenta: ACC-ADMIN-001", doc);
    }

    private void crearUsuarioNormal() {
        String doc = "1000000002";
        if (usuarioRepository.findByNumDocumento(doc).isPresent()) {
            log.info("Usuario normal ya existe, omitiendo creación");
            return;
        }
        Rol rolUser = rolRepository.findByNombre("USER")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("USER").build()));

        Usuario usuario = Usuario.builder()
                .id(new UsuarioId(doc))
                .nombre("Juan")
                .apellido("Pérez")
                .email("juan@fraude.com")
                .passwordHash(passwordEncoder.encode("user123"))
                .rolId(rolUser.getId())
                .estadoId(1)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        usuarioRepository.save(usuario);

        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta("ACC-USER-001")
                .saldo(BigDecimal.valueOf(100000))
                .numDocumento(doc)
                .build();
        cuentaRepository.save(cuenta);

        log.info("Usuario normal creado — doc: {} | pass: user123 | cuenta: ACC-USER-001", doc);
    }
}
