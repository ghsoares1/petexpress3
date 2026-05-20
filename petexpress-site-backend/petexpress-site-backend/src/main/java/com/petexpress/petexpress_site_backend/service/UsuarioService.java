package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.dto.LoginRequest;
import com.petexpress.petexpress_site_backend.dto.UsuarioCadastroRequest;
import com.petexpress.petexpress_site_backend.dto.UsuarioResponse;
import com.petexpress.petexpress_site_backend.model.Usuario;
import com.petexpress.petexpress_site_backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordService passwordService;
    private final EmailService emailService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordService passwordService,
                          EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordService = passwordService;
        this.emailService = emailService;
    }

    public UsuarioResponse cadastrar(UsuarioCadastroRequest request) {
        validarCadastro(request);

        String email = normalizarEmail(request.getEmail());
        String cpf = limpar(request.getCpf());

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail ja cadastrado.");
        }

        if (usuarioRepository.existsByCpf(cpf)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF ja cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(limpar(request.getNome()));
        usuario.setSobrenome(limpar(request.getSobrenome()));
        usuario.setCpf(cpf);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordService.hash(request.getSenha()));
        usuario.setEndereco(limpar(request.getEndereco()));
        usuario.setComplemento(limpar(request.getComplemento()));
        usuario.setBairro(limpar(request.getBairro()));
        usuario.setCep(limpar(request.getCep()));
        usuario.setDataCadastro(LocalDateTime.now());

        Usuario saved = usuarioRepository.save(usuario);
        try {
            if (emailService != null) {
                emailService.sendAccountCreatedEmail(saved.getEmail(), saved.getNome());
            }
        } catch (Exception ex) {
            logger.warn("Cadastro realizado, mas falha no envio de email de confirmacao para {}", saved.getEmail(), ex);
        }

        return UsuarioResponse.from(saved);
    }

    public UsuarioResponse login(LoginRequest request) {
        if (request == null || isBlank(request.getEmail()) || isBlank(request.getSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe e-mail e senha.");
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(request.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha invalidos."));

        if (!passwordService.matches(request.getSenha(), usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha invalidos.");
        }

        return UsuarioResponse.from(usuario);
    }

    public UsuarioResponse buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado."));
    }

    public UsuarioResponse atualizar(Long id, UsuarioCadastroRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado."));

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do usuario nao informados.");
        }

        String cpf = valorObrigatorio(request.getCpf(), "CPF");
        String email = normalizarEmail(valorObrigatorio(request.getEmail(), "E-mail"));

        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail ja cadastrado.");
        }

        if (usuarioRepository.existsByCpfAndIdNot(cpf, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF ja cadastrado.");
        }

        usuario.setNome(valorObrigatorio(request.getNome(), "Nome"));
        usuario.setSobrenome(valorObrigatorio(request.getSobrenome(), "Sobrenome"));
        usuario.setCpf(cpf);
        usuario.setEmail(email);
        usuario.setEndereco(valorObrigatorio(request.getEndereco(), "Endereco"));
        usuario.setComplemento(limpar(request.getComplemento()));
        usuario.setBairro(valorObrigatorio(request.getBairro(), "Bairro"));
        usuario.setCep(valorObrigatorio(request.getCep(), "CEP"));

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    private void validarCadastro(UsuarioCadastroRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do usuario nao informados.");
        }
        valorObrigatorio(request.getNome(), "Nome");
        valorObrigatorio(request.getSobrenome(), "Sobrenome");
        valorObrigatorio(request.getCpf(), "CPF");
        valorObrigatorio(request.getEmail(), "E-mail");
        valorObrigatorio(request.getEndereco(), "Endereco");
        valorObrigatorio(request.getBairro(), "Bairro");
        valorObrigatorio(request.getCep(), "CEP");

        if (isBlank(request.getSenha()) || request.getSenha().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 6 caracteres.");
        }
    }

    private String valorObrigatorio(String value, String fieldName) {
        if (isBlank(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " e obrigatorio.");
        }
        return limpar(value);
    }

    private String normalizarEmail(String email) {
        return limpar(email).toLowerCase();
    }

    private String limpar(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

