package com.adbrand.core.ia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

// Entidad: una fila de consumo_token (V30). Los tokens que gastó una empresa en una generación con IA (HU 17).
@Entity
@Table(name = "consumo_token")
public class ConsumoToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigenConsumo origen;

    @Column(length = 120)
    private String modelo;

    @Column(name = "tokens_entrada", nullable = false)
    private int tokensEntrada;

    @Column(name = "tokens_salida", nullable = false)
    private int tokensSalida;

    @Column(name = "tokens_total", nullable = false)
    private int tokensTotal;

    // true cuando los tokens son estimados (AI_MOCK=true o el servidor no mandó usage).
    @Column(nullable = false)
    private boolean simulado;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    @PrePersist
    void alCrear() {
        fecha = OffsetDateTime.now();
    }

    public Long getId() { return id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public OrigenConsumo getOrigen() { return origen; }
    public void setOrigen(OrigenConsumo origen) { this.origen = origen; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public int getTokensEntrada() { return tokensEntrada; }
    public void setTokensEntrada(int tokensEntrada) { this.tokensEntrada = tokensEntrada; }

    public int getTokensSalida() { return tokensSalida; }
    public void setTokensSalida(int tokensSalida) { this.tokensSalida = tokensSalida; }

    public int getTokensTotal() { return tokensTotal; }
    public void setTokensTotal(int tokensTotal) { this.tokensTotal = tokensTotal; }

    public boolean isSimulado() { return simulado; }
    public void setSimulado(boolean simulado) { this.simulado = simulado; }

    public OffsetDateTime getFecha() { return fecha; }
}
