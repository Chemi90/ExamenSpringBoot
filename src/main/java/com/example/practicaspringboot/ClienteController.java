package com.example.practicaspringboot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    // Suponiendo que hay un servicio de seguridad similar al ejemplo proporcionado
    @Autowired
    private SecurityService security;

    // Introducir un nuevo cliente
    @PostMapping("/")
    public ResponseEntity<?> crearCliente(@RequestBody Cliente cliente, @RequestParam String token) {
        if (!security.validateToken(token)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Cliente nuevoCliente = clienteRepository.save(cliente);
        return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
    }

    // Devolver todos los datos de un cliente
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerClientePorId(@PathVariable Long id, @RequestParam String token) {
        if (!security.validateToken(token)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Optional<Cliente> cliente = clienteRepository.findById(id);
        return cliente.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Devolver los clientes con un número de ventas mayor a un parámetro dado
    @GetMapping("/ventas")
    public ResponseEntity<?> obtenerClientesPorVentas(@RequestParam BigDecimal ventas, @RequestParam String token) {
        if (!security.validateToken(token)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<Cliente> clientes = clienteRepository.findAll().stream()
                .filter(c -> c.getTotal().compareTo(ventas) > 0)
                .collect(Collectors.toList());
        return new ResponseEntity<>(clientes, HttpStatus.OK);
    }

    // Mostrar en un JSON el resumen estadístico
    @GetMapping("/estadisticas")
    public ResponseEntity<?> obtenerEstadisticas(@RequestParam String token) {
        if (!security.validateToken(token)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<Cliente> clientes = clienteRepository.findAll();
        BigDecimal totalVentas = clientes.stream()
                .map(Cliente::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Asegúrate de que no divides por cero si no hay clientes activos
        BigDecimal promedioVentasActivos = clientes.stream()
                .filter(c -> "activo".equals(c.getEstado()))
                .map(Cliente::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(clientes.stream().filter(c -> "activo".equals(c.getEstado())).count()), BigDecimal.ROUND_HALF_UP);
        long cantidadInactivos = clientes.stream()
                .filter(c -> "inactivo".equals(c.getEstado()) && c.getTotal().compareTo(BigDecimal.ZERO) > 0)
                .count();
        Estadisticas estadisticas = new Estadisticas(totalVentas, promedioVentasActivos, cantidadInactivos);

        return new ResponseEntity<>(estadisticas, HttpStatus.OK);
    }

    // Clase interna para representar las estadísticas
    static class Estadisticas {
        private final BigDecimal totalVentas;
        private final BigDecimal promedioVentasActivos;
        private final long cantidadInactivos;

        public Estadisticas(BigDecimal totalVentas, BigDecimal promedioVentasActivos, long cantidadInactivos) {
            this.totalVentas = totalVentas;
            this.promedioVentasActivos = promedioVentasActivos;
            this.cantidadInactivos = cantidadInactivos;
        }

        // Getters para que Spring Boot pueda serializar los valores a JSON
        public BigDecimal getTotalVentas() {
            return totalVentas;
        }

        public BigDecimal getPromedioVentasActivos() {
            return promedioVentasActivos;
        }

        public long getCantidadInactivos() {
            return cantidadInactivos;
        }
    }
}
