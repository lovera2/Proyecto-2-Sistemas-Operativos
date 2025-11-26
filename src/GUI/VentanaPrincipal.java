/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package GUI;

import Componentes.SistemaArchivos;
import Componentes.GestorDisco;
import Procesos.Planificador;
import Procesos.GestorProcesoES;
import Componentes.Archivo;
import Componentes.Bloque;
import Componentes.Directorio;
import Componentes.SimulacionDiscoSD;
import Estructuras.Nodo;

import javax.swing.table.DefaultTableModel;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.JOptionPane;

/**
 *
 * @author luismarianolovera
 */
public class VentanaPrincipal extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(VentanaPrincipal.class.getName());

    private SistemaArchivos sistemaArchivos;
    private GestorDisco gestorDisco;
    private Planificador planificador;
    private GestorProcesoES gestorProcesoES;
    private String rutaDirectorioSeleccionado = "/"; // ruta del directorio padre
    private String nombreSeleccionado = null;        // nombre del archivo/dir seleccionado
    private boolean seleccionEsDirectorio = true;    // true = directorio, false = archivo
    private Directorio directorioSeleccionado;
    private Archivo archivoSeleccionado;

    // Disco visual (64 bloques)
    private JLabel[] bloquesDisco;   // 64 labels para el SD

    // Timer para la simulación de E/S
    private Timer timerSimulacion;

    /**
     * Creates new form Ventana
     */
    public VentanaPrincipal() {
        initComponents();
        inicializarModelo();
        inicializarDiscoVisual();
        inicializarTablas();
        inicializarTimer();
        inicializarEventosArbol();
        configurarModo();          // <<< NUEVO: configurar botones según modo
        refrescarArbol();
        refrescarDiscoVisual();
        refrescarTablaAsignacion();
        configurarTipoObjeto();
    }

    // ================== HELPERS DE MENSAJES ==================

    /** Mensajes que sí van al log de sistema (detalles técnicos). */
    private void logSistema(String mensaje) {
        txtLogSistema.append(mensaje + "\n");
        txtLogSistema.setCaretPosition(txtLogSistema.getDocument().getLength());
    }

    /** Mensajes que se muestran al usuario en un JOptionPane. */
    private void mostrarMensajeUsuario(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Mensaje",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ================== MODO USUARIO / ADMIN ==================

    private void configurarModo() {
        rbAdmin.addActionListener(e -> toggleModo(true));
        rbUsuario.addActionListener(e -> toggleModo(false));
        // Arranca en lo que ya esté seleccionado (inicializarModelo marca Usuario)
        toggleModo(rbAdmin.isSelected());
    }

    /** Habilita o deshabilita acciones solo-admin. */
    private void toggleModo(boolean admin) {
        btnCrear.setEnabled(admin);
        comboTipoObjeto.setEnabled(admin);
        btnEliminarSeleccion.setEnabled(admin);
        btnRenombrarSeleccion.setEnabled(admin);
        txtNuevoNombre.setEnabled(admin);
        txtNombre.setEnabled(admin);
        spinTamanoBloques.setEnabled(admin);
        // leer siempre permitido
    }

    // ==========================================================
    // ===================== INICIALIZACIÓN =====================
    // ==========================================================

    private void inicializarModelo() {
        // 64 bloques en el SD
        int cantidadBloques = 64;

        // 1) Sistema de archivos
        sistemaArchivos = new SistemaArchivos(cantidadBloques);

        // 2) Gestor de disco (recibe el sistema de archivos)
        gestorDisco = new GestorDisco(sistemaArchivos);

        // 3) Planificador inicial (FIFO por defecto)
        planificador = new Planificador();

        // 4) Gestor de procesos de E/S
        gestorProcesoES = new GestorProcesoES(planificador, gestorDisco);

        // 5) Por defecto, modo usuario
        grupoModo.add(rbUsuario);
        grupoModo.add(rbAdmin);
        rbUsuario.setSelected(true);
    }

    private void inicializarDiscoVisual() {
        int filas = 8;
        int columnas = 8;
        int cantidadBloques = filas * columnas;

        panelDisco1.removeAll();
        panelDisco1.setLayout(new java.awt.GridLayout(filas, columnas, 4, 4));

        bloquesDisco = new javax.swing.JLabel[cantidadBloques];

        for (int i = 0; i < cantidadBloques; i++) {
            javax.swing.JLabel lbl = new javax.swing.JLabel(String.valueOf(i), javax.swing.SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBackground(java.awt.Color.LIGHT_GRAY); // libre
            lbl.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.DARK_GRAY));
            lbl.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.PLAIN, 11));

            bloquesDisco[i] = lbl;
            panelDisco1.add(lbl);
        }

        panelDisco1.revalidate();
        panelDisco1.repaint();
    }

    private void inicializarTablas() {
    // ================== TABLA ASIGNACIÓN ARCHIVOS ==================
    DefaultTableModel modeloAsignacion =
        new DefaultTableModel(
            new Object[][]{},
            new String[]{"Color", "Nombre", "Cant. Bloques", "Dirección 1er bloque", "Proceso creador"}
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    tablaAsignacion.setModel(modeloAsignacion);
    
        // Render para que la primera columna muestre un cuadrito de color
        tablaAsignacion.getColumnModel().getColumn(0)
            .setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
        public java.awt.Component getTableCellRendererComponent(
            javax.swing.JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {

        // texto vacío, solo el fondo
        java.awt.Component c = super.getTableCellRendererComponent(
                table, "", isSelected, hasFocus, row, column);

        if (value instanceof java.awt.Color) {
            c.setBackground((java.awt.Color) value);
        } else {
            c.setBackground(java.awt.Color.LIGHT_GRAY);
        }

        setOpaque(true);
        return c;
        }
    });

    // ================== MODELO COMÚN PARA TODAS LAS COLAS DE PROCESOS ==================
    DefaultTableModel modeloNuevos =
        new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID", "Operación", "Ruta", "Estado", "tES", "Pista"}
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

    DefaultTableModel modeloListos =
        new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID", "Operación", "Ruta", "Estado", "tES", "Pista"}
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

    DefaultTableModel modeloEjecutados =
        new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID", "Operación", "Ruta", "Estado", "tES", "Pista"}
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

    DefaultTableModel modeloBloqueados =
        new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID", "Operación", "Ruta", "Estado", "tES", "Pista"}
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

    DefaultTableModel modeloTerminados =
        new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID", "Operación", "Ruta", "Estado", "tES", "Pista"}
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

    jTableNuevos.setModel(modeloNuevos);
    jTableListos.setModel(modeloListos);
    jTableEjecutados.setModel(modeloEjecutados);
    jTableBloqueados.setModel(modeloBloqueados);
    jTableTerminados.setModel(modeloTerminados);
}
    
    private void cargarDatosEnTabla(javax.swing.JTable tabla, Object[][] datos) {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        modelo.setRowCount(0);
        if (datos == null) return;
        for (Object[] fila : datos) {
        modelo.addRow(fila);
            }
}

    private void inicializarTimer() {
        int delayMs = 800; // medio segundo por "tick" de E/S

        timerSimulacion = new javax.swing.Timer(delayMs, e -> {
            ejecutarTickSimulacion();
        });
    }

    private void actualizarTablasProcesos() {
    // Cada método lo implementas en GestorProcesoES
    Object[][] nuevos      = gestorProcesoES.obtenerTablaNuevos();
    Object[][] listos      = gestorProcesoES.obtenerTablaListos();
    Object[][] ejecutados  = gestorProcesoES.obtenerTablaEjecutados();
    Object[][] bloqueados  = gestorProcesoES.obtenerTablaBloqueados();
    Object[][] terminados  = gestorProcesoES.obtenerTablaTerminados();

    cargarDatosEnTabla(jTableNuevos,     nuevos);
    cargarDatosEnTabla(jTableListos,     listos);
    cargarDatosEnTabla(jTableEjecutados, ejecutados);
    cargarDatosEnTabla(jTableBloqueados, bloqueados);
    cargarDatosEnTabla(jTableTerminados, terminados);
}

    private void inicializarEventosArbol() {
        arbolSistema.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                actualizarInfoSeleccion();
            }
        });
    }

    private void refrescarArbol() {
        if (sistemaArchivos == null) return;

        Directorio raiz = sistemaArchivos.getRaiz();
        DefaultMutableTreeNode nodoRaiz = new DefaultMutableTreeNode(raiz);

        construirNodosDirectorio(raiz, nodoRaiz);

        DefaultTreeModel modelo = new DefaultTreeModel(nodoRaiz);
        arbolSistema.setModel(modelo);
        arbolSistema.expandRow(0);
    }

    private void construirNodosDirectorio(Directorio dir, DefaultMutableTreeNode nodoPadre) {
        // Subdirectorios
        Estructuras.Nodo nodoSub = dir.getSubdirectorios().getFirst();
        while (nodoSub != null) {
            Directorio sub = (Directorio) nodoSub.getDato();
            DefaultMutableTreeNode nodoSubDir = new DefaultMutableTreeNode(sub);
            nodoPadre.add(nodoSubDir);

            construirNodosDirectorio(sub, nodoSubDir);  // recursivo
            nodoSub = nodoSub.getNext();
        }

        // Archivos
        Estructuras.Nodo nodoArch = dir.getArchivos().getFirst();
        while (nodoArch != null) {
            Archivo a = (Archivo) nodoArch.getDato();
            DefaultMutableTreeNode nodoArchivo = new DefaultMutableTreeNode(a);
            nodoPadre.add(nodoArchivo);
            nodoArch = nodoArch.getNext();
        }
    }

    private void actualizarInfoSeleccion() {
        javax.swing.tree.TreePath path = arbolSistema.getSelectionPath();
        directorioSeleccionado = null;
        archivoSeleccionado = null;

        if (path == null) {
            lblRutaSeleccionada.setText("-/-");
            lblTipoSeleccionado.setText("-/-");
            lblTamanoSeleccionado.setText("-/-");
            lblSeleccionadoActual.setText("-/-");
            return;
        }

        DefaultMutableTreeNode nodo = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object obj = nodo.getUserObject();

        if (obj instanceof Directorio) {
            Directorio dir = (Directorio) obj;
            directorioSeleccionado = dir;
            String ruta = construirRutaDirectorio(dir);

            lblRutaSeleccionada.setText(ruta);
            lblTipoSeleccionado.setText("Directorio");
            lblTamanoSeleccionado.setText("-");  // puedes sumar archivos si quieres
            lblSeleccionadoActual.setText(dir.getNombre());
        } else if (obj instanceof Archivo) {
            Archivo a = (Archivo) obj;
            archivoSeleccionado = a;

            // Ruta = ruta del directorio padre + /nombreArchivo
            DefaultMutableTreeNode nodoPadre = (DefaultMutableTreeNode) nodo.getParent();
            Directorio dirPadre = (Directorio) nodoPadre.getUserObject();
            String rutaDir = construirRutaDirectorio(dirPadre);
            String rutaCompleta = ("/".equals(rutaDir) ? "/" + a.getNombre() : rutaDir + "/" + a.getNombre());

            lblRutaSeleccionada.setText(rutaCompleta);
            lblTipoSeleccionado.setText("Archivo");
            lblTamanoSeleccionado.setText(String.valueOf(a.getTamañoEnBloques()));
            lblSeleccionadoActual.setText(a.getNombre());
        }
    }

    private String construirRutaDirectorio(Directorio dir) {
        if (dir.getPadre() == null) {
            return "/";
        }
        String rutaPadre = construirRutaDirectorio(dir.getPadre());
        if ("/".equals(rutaPadre)) {
            return rutaPadre + dir.getNombre();
        }
        return rutaPadre + "/" + dir.getNombre();
    }

    private void refrescarDiscoVisual() {
    if (sistemaArchivos == null || bloquesDisco == null) return;

    SimulacionDiscoSD disco = sistemaArchivos.getDisco();
    if (disco == null) return;

    int n = disco.getCantidadBloques();

    for (int i = 0; i < bloquesDisco.length; i++) {
        JLabel lbl = bloquesDisco[i];

        if (i < n) {
            Bloque b = disco.getBloque(i);

            if (b != null && b.isOcupado()) {
                Color c = b.getColorBloque();
                if (c == null) c = Color.CYAN;   // fallback si algo raro pasó
                lbl.setBackground(c);
            } else {
                lbl.setBackground(Color.LIGHT_GRAY);
            }
        } else {
            lbl.setBackground(Color.LIGHT_GRAY);
        }
    }
}

    private void refrescarTablaAsignacion() {
        javax.swing.table.DefaultTableModel modelo =
            (javax.swing.table.DefaultTableModel) tablaAsignacion.getModel();
        modelo.setRowCount(0);

        if (sistemaArchivos == null) return;

        agregarArchivosATabla(sistemaArchivos.getRaiz(), modelo);
    }

    private void agregarArchivosATabla(Directorio dir, javax.swing.table.DefaultTableModel modelo) {
        // Archivos del directorio actual
        Estructuras.Nodo nodoArch = dir.getArchivos().getFirst();
        while (nodoArch != null) {
            Archivo a = (Archivo) nodoArch.getDato();
            Object[] fila = new Object[] {
                a.getColorArchivo(),           // Color lógico
                a.getNombre(),
                a.getTamañoEnBloques(),
                a.getPrimerBloque(),
                a.getIdProcesoCreador()
            };
            modelo.addRow(fila);
            nodoArch = nodoArch.getNext();
        }

        // Subdirectorios recursivos
        Estructuras.Nodo nodoSub = dir.getSubdirectorios().getFirst();
        while (nodoSub != null) {
            Directorio sub = (Directorio) nodoSub.getDato();
            agregarArchivosATabla(sub, modelo);
            nodoSub = nodoSub.getNext();
        }
    }

    private void refrescarProcesosYDisco() {
        actualizarTablasProcesos();
        refrescarDiscoVisual();
    }
    
    private void ejecutarTickSimulacion() {
        gestorProcesoES.admitirNuevosAListos(); // NUEVOS -> LISTOS
        gestorProcesoES.tickEjecucion();        // EJECUCIÓN -> BLOQUEADOS
        gestorProcesoES.tickBloqueados();       // BLOQUEADOS -> TERMINADOS (+ disco)
        gestorProcesoES.despacharSiguiente();   // LISTOS -> EJECUCIÓN

    // luego refrescas TODAS las tablas:
        actualizarTablasProcesos();             // nuevos, listos, ejec, bloq, term
        refrescarDiscoVisual();
        refrescarTablaAsignacion();
        //refrescarArbol();
    }

    // Habilita / deshabilita el tamaño según el tipo seleccionado
    private void configurarTipoObjeto() {
        // estado inicial al abrir la ventana
        actualizarHabilitarTamano();

        comboTipoObjeto.addActionListener(e -> actualizarHabilitarTamano());
    }

    private void actualizarHabilitarTamano() {
        String tipo = (String) comboTipoObjeto.getSelectedItem();
        boolean esArchivo = "Archivo".equals(tipo);

        spinTamanoBloques.setEnabled(esArchivo);
        jLabel6.setEnabled(esArchivo); // etiqueta "Tamaño (cant. de bloques):"
    }
    
    private boolean existeNombreEnDirectorio(Directorio dir, String nombreBuscado) {
        // Revisar archivos
        Estructuras.Nodo nodoArch = dir.getArchivos().getFirst();
        while (nodoArch != null) {
            Archivo a = (Archivo) nodoArch.getDato();
            if (a.getNombre().equals(nombreBuscado)) {
                return true;
            }
            nodoArch = nodoArch.getNext();
        }

        // Revisar subdirectorios
        Estructuras.Nodo nodoSub = dir.getSubdirectorios().getFirst();
        while (nodoSub != null) {
            Directorio d = (Directorio) nodoSub.getDato();
            if (d.getNombre().equals(nombreBuscado)) {
                return true;
            }
            nodoSub = nodoSub.getNext();
        }

        return false;
}

    private void comboPoliticaActionPerformed(java.awt.event.ActionEvent evt) {
        String politica = (String) comboPolitica.getSelectedItem();
        if (planificador != null && politica != null) {
            planificador.setPolitica(politica);
            logSistema("Política de disco cambiada a: " + politica);
        }
    }

    private void btnEliminarSeleccionActionPerformed(java.awt.event.ActionEvent evt) {
        if (!rbAdmin.isSelected()) {
            mostrarMensajeUsuario("Solo el modo Administrador puede eliminar.");
            return;
        }

        if (archivoSeleccionado != null) {
            // Eliminar archivo mediante proceso de E/S
            String rutaArchivo = lblRutaSeleccionada.getText();
            int pista = 0; // por ahora fijo
            gestorProcesoES.crearProcesoEliminar(rutaArchivo, pista);
            logSistema("Se creó proceso ELIMINAR para " + rutaArchivo);
        } else if (directorioSeleccionado != null && directorioSeleccionado.getPadre() != null) {
            String rutaDir = construirRutaDirectorio(directorioSeleccionado);
            boolean ok = sistemaArchivos.eliminarDirectorio(rutaDir);
            if (ok) {
                logSistema("Directorio eliminado: " + rutaDir);
            } else {
                mostrarMensajeUsuario("No se pudo eliminar el directorio.");
            }
        } else {
            mostrarMensajeUsuario("No hay nada seleccionado.");
            return;
        }

        refrescarArbol();
        refrescarTablaAsignacion();
        refrescarProcesosYDisco();
        
       
}
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        grupoModo = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        panelTablaAsignacion = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        tablaAsignacion = new javax.swing.JTable();
        panelCrear = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        comboTipoObjeto = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        spinTamanoBloques = new javax.swing.JSpinner();
        btnCrear = new javax.swing.JButton();
        panelOperarSeleccion = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        txtNuevoNombre = new javax.swing.JTextField();
        btnEliminarSeleccion = new javax.swing.JButton();
        lblSeleccionadoActual = new javax.swing.JLabel();
        btnLeerSeleccion = new javax.swing.JButton();
        btnRenombrarSeleccion = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        rbUsuario = new javax.swing.JRadioButton();
        rbAdmin = new javax.swing.JRadioButton();
        panelArchivoTexto = new javax.swing.JPanel();
        btnCargar = new javax.swing.JButton();
        btnGuardarEstado = new javax.swing.JButton();
        panelBotonInicio = new javax.swing.JPanel();
        btnIniciar = new javax.swing.JButton();
        btnPausar = new javax.swing.JButton();
        panelDisco = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        txtLogSistema = new javax.swing.JTextArea();
        btnReiniciar = new javax.swing.JButton();
        panelDisco1 = new javax.swing.JPanel();
        panelZonaArbol = new javax.swing.JPanel();
        panelArbol = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        arbolSistema = new javax.swing.JTree();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        lblRutaSeleccionada = new javax.swing.JLabel();
        lblTipoSeleccionado = new javax.swing.JLabel();
        lblTamanoSeleccionado = new javax.swing.JLabel();
        panelArchivoTexto1 = new javax.swing.JPanel();
        comboPolitica = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        panelTablaProcesos7 = new javax.swing.JPanel();
        tablaProcesos7 = new javax.swing.JScrollPane();
        jTableNuevos = new javax.swing.JTable();
        panelTablaProcesos10 = new javax.swing.JPanel();
        tablaProcesos10 = new javax.swing.JScrollPane();
        jTableListos = new javax.swing.JTable();
        panelTablaProcesos11 = new javax.swing.JPanel();
        tablaProcesos11 = new javax.swing.JScrollPane();
        jTableEjecutados = new javax.swing.JTable();
        panelTablaProcesos12 = new javax.swing.JPanel();
        tablaProcesos12 = new javax.swing.JScrollPane();
        jTableBloqueados = new javax.swing.JTable();
        panelTablaProcesos13 = new javax.swing.JPanel();
        tablaProcesos13 = new javax.swing.JScrollPane();
        jTableTerminados = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(204, 204, 255));
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setLayout(new java.awt.BorderLayout());
        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 180, -1, -1));

        panelTablaAsignacion.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Tabla de asignación de archivos", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        tablaAsignacion.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Color", "Nombre", "Cant. Bloques", "Dirección del 1er bloque"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(tablaAsignacion);

        jScrollPane2.setViewportView(jScrollPane3);

        javax.swing.GroupLayout panelTablaAsignacionLayout = new javax.swing.GroupLayout(panelTablaAsignacion);
        panelTablaAsignacion.setLayout(panelTablaAsignacionLayout);
        panelTablaAsignacionLayout.setHorizontalGroup(
            panelTablaAsignacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTablaAsignacionLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelTablaAsignacionLayout.setVerticalGroup(
            panelTablaAsignacionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTablaAsignacionLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
        );

        getContentPane().add(panelTablaAsignacion, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 650, 400, 250));

        panelCrear.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Crear elemento", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        jLabel4.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel4.setText("Tipo de objeto:");

        comboTipoObjeto.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Archivo", "Directorio" }));

        jLabel5.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel5.setText("Nombre:");

        txtNombre.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNombreActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel6.setText("Tamaño (cant. de bloques):");

        spinTamanoBloques.setModel(new javax.swing.SpinnerNumberModel(1, 1, 1000, 1));

        btnCrear.setText("Crear");
        btnCrear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCrearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelCrearLayout = new javax.swing.GroupLayout(panelCrear);
        panelCrear.setLayout(panelCrearLayout);
        panelCrearLayout.setHorizontalGroup(
            panelCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCrearLayout.createSequentialGroup()
                .addGroup(panelCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelCrearLayout.createSequentialGroup()
                        .addGroup(panelCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelCrearLayout.createSequentialGroup()
                                .addGap(92, 92, 92)
                                .addGroup(panelCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel5))
                                .addGap(21, 21, 21))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelCrearLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel6)
                                .addGap(18, 18, 18)))
                        .addGroup(panelCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtNombre)
                            .addComponent(comboTipoObjeto, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(spinTamanoBloques, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(panelCrearLayout.createSequentialGroup()
                        .addGap(134, 134, 134)
                        .addComponent(btnCrear)))
                .addContainerGap(42, Short.MAX_VALUE))
        );
        panelCrearLayout.setVerticalGroup(
            panelCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCrearLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(panelCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboTipoObjeto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addGap(12, 12, 12)
                .addGroup(panelCrearLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spinTamanoBloques, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addGap(21, 21, 21)
                .addComponent(btnCrear)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(panelCrear, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 190, 360, 180));

        panelOperarSeleccion.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Interactuar con elemento", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        jLabel7.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel7.setText("Elemento seleccionado:");

        jLabel8.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel8.setText("Nuevo nombre:");

        txtNuevoNombre.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNuevoNombreActionPerformed(evt);
            }
        });

        btnEliminarSeleccion.setText("Eliminar selección");

        lblSeleccionadoActual.setText("-/-");

        btnLeerSeleccion.setText("Leer selección");
        btnLeerSeleccion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLeerSeleccionActionPerformed(evt);
            }
        });

        btnRenombrarSeleccion.setText("Renombrar selección");
        btnRenombrarSeleccion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRenombrarSeleccionActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelOperarSeleccionLayout = new javax.swing.GroupLayout(panelOperarSeleccion);
        panelOperarSeleccion.setLayout(panelOperarSeleccionLayout);
        panelOperarSeleccionLayout.setHorizontalGroup(
            panelOperarSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelOperarSeleccionLayout.createSequentialGroup()
                .addGroup(panelOperarSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelOperarSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(panelOperarSeleccionLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(21, 21, 21)
                            .addComponent(lblSeleccionadoActual, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(panelOperarSeleccionLayout.createSequentialGroup()
                            .addGroup(panelOperarSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(panelOperarSeleccionLayout.createSequentialGroup()
                                    .addGap(0, 0, Short.MAX_VALUE)
                                    .addComponent(jLabel8))
                                .addGroup(panelOperarSeleccionLayout.createSequentialGroup()
                                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnLeerSeleccion)))
                            .addGap(31, 31, 31)
                            .addGroup(panelOperarSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnEliminarSeleccion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(txtNuevoNombre))
                            .addGap(28, 28, 28)))
                    .addGroup(panelOperarSeleccionLayout.createSequentialGroup()
                        .addGap(96, 96, 96)
                        .addComponent(btnRenombrarSeleccion)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelOperarSeleccionLayout.setVerticalGroup(
            panelOperarSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelOperarSeleccionLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(panelOperarSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(lblSeleccionadoActual))
                .addGap(18, 18, 18)
                .addGroup(panelOperarSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnEliminarSeleccion)
                    .addComponent(btnLeerSeleccion, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(19, 19, 19)
                .addGroup(panelOperarSeleccionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(txtNuevoNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(btnRenombrarSeleccion, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        getContentPane().add(panelOperarSeleccion, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 400, -1, 190));

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Modo de uso", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        jLabel9.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel9.setText("Seleccione un modo de uso:");

        rbUsuario.setText("Modo Usuario");
        rbUsuario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbUsuarioActionPerformed(evt);
            }
        });

        grupoModo.add(rbAdmin);
        rbAdmin.setText("Modo Administrador");
        rbAdmin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbAdminActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(rbUsuario)
                        .addGap(66, 66, 66)
                        .addComponent(rbAdmin)))
                .addContainerGap(19, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbUsuario)
                    .addComponent(rbAdmin))
                .addContainerGap(7, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, 360, 90));

        panelArchivoTexto.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Uso de archivos (JSON)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        btnCargar.setText("Cargar archivo");

        btnGuardarEstado.setText("Guardar archivo");
        btnGuardarEstado.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarEstadoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelArchivoTextoLayout = new javax.swing.GroupLayout(panelArchivoTexto);
        panelArchivoTexto.setLayout(panelArchivoTextoLayout);
        panelArchivoTextoLayout.setHorizontalGroup(
            panelArchivoTextoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelArchivoTextoLayout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(btnCargar, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(36, 36, 36)
                .addComponent(btnGuardarEstado)
                .addContainerGap(29, Short.MAX_VALUE))
        );
        panelArchivoTextoLayout.setVerticalGroup(
            panelArchivoTextoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelArchivoTextoLayout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addGroup(panelArchivoTextoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCargar)
                    .addComponent(btnGuardarEstado))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(panelArchivoTexto, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 680, 360, 60));

        panelBotonInicio.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Control de Simulación", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        btnIniciar.setText("Iniciar");
        btnIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIniciarActionPerformed(evt);
            }
        });

        btnPausar.setText("Pausar");
        btnPausar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPausarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelBotonInicioLayout = new javax.swing.GroupLayout(panelBotonInicio);
        panelBotonInicio.setLayout(panelBotonInicioLayout);
        panelBotonInicioLayout.setHorizontalGroup(
            panelBotonInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBotonInicioLayout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(btnIniciar, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(41, 41, 41)
                .addComponent(btnPausar, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(29, Short.MAX_VALUE))
        );
        panelBotonInicioLayout.setVerticalGroup(
            panelBotonInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBotonInicioLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelBotonInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnIniciar)
                    .addComponent(btnPausar))
                .addContainerGap(7, Short.MAX_VALUE))
        );

        getContentPane().add(panelBotonInicio, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 760, 360, 60));

        panelDisco.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        panelDisco.setLayout(new java.awt.GridLayout(1, 0));
        getContentPane().add(panelDisco, new org.netbeans.lib.awtextra.AbsoluteConstraints(8, 8, 2, 2));

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Logs del sistema:", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        txtLogSistema.setEditable(false);
        txtLogSistema.setBackground(new java.awt.Color(255, 255, 255));
        txtLogSistema.setColumns(20);
        txtLogSistema.setLineWrap(true);
        txtLogSistema.setRows(5);
        txtLogSistema.setWrapStyleWord(true);
        jScrollPane4.setViewportView(txtLogSistema);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
        );

        getContentPane().add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 790, 530, 120));

        btnReiniciar.setText("Reiniciar sistema");
        btnReiniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReiniciarActionPerformed(evt);
            }
        });
        getContentPane().add(btnReiniciar, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 870, -1, -1));

        panelDisco1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout panelDisco1Layout = new javax.swing.GroupLayout(panelDisco1);
        panelDisco1.setLayout(panelDisco1Layout);
        panelDisco1Layout.setHorizontalGroup(
            panelDisco1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 388, Short.MAX_VALUE)
        );
        panelDisco1Layout.setVerticalGroup(
            panelDisco1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 288, Short.MAX_VALUE)
        );

        getContentPane().add(panelDisco1, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 340, 390, 290));

        panelZonaArbol.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Árbol (JTree)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        jScrollPane1.setViewportView(arbolSistema);

        javax.swing.GroupLayout panelArbolLayout = new javax.swing.GroupLayout(panelArbol);
        panelArbol.setLayout(panelArbolLayout);
        panelArbolLayout.setHorizontalGroup(
            panelArbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelArbolLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 312, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelArbolLayout.setVerticalGroup(
            panelArbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelArbolLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel1.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel1.setText("Ruta:");

        jLabel2.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel2.setText("Tipo:");

        jLabel3.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel3.setText("Tamaño (bloques):");

        lblRutaSeleccionada.setText("-/-");

        lblTipoSeleccionado.setText("-/-");

        lblTamanoSeleccionado.setText("-/-");

        javax.swing.GroupLayout panelZonaArbolLayout = new javax.swing.GroupLayout(panelZonaArbol);
        panelZonaArbol.setLayout(panelZonaArbolLayout);
        panelZonaArbolLayout.setHorizontalGroup(
            panelZonaArbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelZonaArbolLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(panelZonaArbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelArbol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelZonaArbolLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(panelZonaArbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(36, 36, 36)
                        .addGroup(panelZonaArbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblRutaSeleccionada, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE)
                            .addComponent(lblTipoSeleccionado, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblTamanoSeleccionado, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelZonaArbolLayout.setVerticalGroup(
            panelZonaArbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelZonaArbolLayout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(panelArbol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(panelZonaArbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(lblRutaSeleccionada))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelZonaArbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(lblTipoSeleccionado))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelZonaArbolLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lblTamanoSeleccionado))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(panelZonaArbol, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 30, 390, 290));

        panelArchivoTexto1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Implementación de política", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        comboPolitica.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "FIFO", "SSTF", "SCAN", "C-SCAN" }));

        jLabel10.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel10.setText("Escoger política de disco: ");

        javax.swing.GroupLayout panelArchivoTexto1Layout = new javax.swing.GroupLayout(panelArchivoTexto1);
        panelArchivoTexto1.setLayout(panelArchivoTexto1Layout);
        panelArchivoTexto1Layout.setHorizontalGroup(
            panelArchivoTexto1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelArchivoTexto1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(35, 35, 35)
                .addComponent(comboPolitica, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(39, 39, 39))
        );
        panelArchivoTexto1Layout.setVerticalGroup(
            panelArchivoTexto1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelArchivoTexto1Layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(panelArchivoTexto1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(comboPolitica, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(panelArchivoTexto1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 600, 360, 60));

        panelTablaProcesos7.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Cola de Nuevos", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        jTableNuevos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Operación", "Ruta", "Estado", "tES", "Pista"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaProcesos7.setViewportView(jTableNuevos);

        javax.swing.GroupLayout panelTablaProcesos7Layout = new javax.swing.GroupLayout(panelTablaProcesos7);
        panelTablaProcesos7.setLayout(panelTablaProcesos7Layout);
        panelTablaProcesos7Layout.setHorizontalGroup(
            panelTablaProcesos7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTablaProcesos7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tablaProcesos7, javax.swing.GroupLayout.DEFAULT_SIZE, 498, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelTablaProcesos7Layout.setVerticalGroup(
            panelTablaProcesos7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTablaProcesos7Layout.createSequentialGroup()
                .addContainerGap(15, Short.MAX_VALUE)
                .addComponent(tablaProcesos7, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19))
        );

        getContentPane().add(panelTablaProcesos7, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 30, 520, 150));

        panelTablaProcesos10.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Cola de Listos", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        jTableListos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Operación", "Ruta", "Estado", "tES", "Pista"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaProcesos10.setViewportView(jTableListos);

        javax.swing.GroupLayout panelTablaProcesos10Layout = new javax.swing.GroupLayout(panelTablaProcesos10);
        panelTablaProcesos10.setLayout(panelTablaProcesos10Layout);
        panelTablaProcesos10Layout.setHorizontalGroup(
            panelTablaProcesos10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTablaProcesos10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tablaProcesos10, javax.swing.GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelTablaProcesos10Layout.setVerticalGroup(
            panelTablaProcesos10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTablaProcesos10Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(tablaProcesos10, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32))
        );

        getContentPane().add(panelTablaProcesos10, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 200, -1, 140));

        panelTablaProcesos11.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Cola de Ejecutados", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        jTableEjecutados.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Operación", "Ruta", "Estado", "tES", "Pista"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaProcesos11.setViewportView(jTableEjecutados);

        javax.swing.GroupLayout panelTablaProcesos11Layout = new javax.swing.GroupLayout(panelTablaProcesos11);
        panelTablaProcesos11.setLayout(panelTablaProcesos11Layout);
        panelTablaProcesos11Layout.setHorizontalGroup(
            panelTablaProcesos11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTablaProcesos11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tablaProcesos11, javax.swing.GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelTablaProcesos11Layout.setVerticalGroup(
            panelTablaProcesos11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTablaProcesos11Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(tablaProcesos11, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
        );

        getContentPane().add(panelTablaProcesos11, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 360, -1, 130));

        panelTablaProcesos12.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Cola de Bloqueados", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        jTableBloqueados.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Operación", "Ruta", "Estado", "tES", "Pista"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaProcesos12.setViewportView(jTableBloqueados);

        javax.swing.GroupLayout panelTablaProcesos12Layout = new javax.swing.GroupLayout(panelTablaProcesos12);
        panelTablaProcesos12.setLayout(panelTablaProcesos12Layout);
        panelTablaProcesos12Layout.setHorizontalGroup(
            panelTablaProcesos12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTablaProcesos12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tablaProcesos12, javax.swing.GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelTablaProcesos12Layout.setVerticalGroup(
            panelTablaProcesos12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTablaProcesos12Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(tablaProcesos12, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(127, 127, 127))
        );

        getContentPane().add(panelTablaProcesos12, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 510, -1, 130));

        panelTablaProcesos13.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Cola de Terminados", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Helvetica Neue", 3, 13))); // NOI18N

        jTableTerminados.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Operación", "Ruta", "Estado", "tES", "Pista"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaProcesos13.setViewportView(jTableTerminados);

        javax.swing.GroupLayout panelTablaProcesos13Layout = new javax.swing.GroupLayout(panelTablaProcesos13);
        panelTablaProcesos13.setLayout(panelTablaProcesos13Layout);
        panelTablaProcesos13Layout.setHorizontalGroup(
            panelTablaProcesos13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTablaProcesos13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tablaProcesos13, javax.swing.GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelTablaProcesos13Layout.setVerticalGroup(
            panelTablaProcesos13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTablaProcesos13Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(tablaProcesos13, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19))
        );

        getContentPane().add(panelTablaProcesos13, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 660, 530, 120));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtNombreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNombreActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNombreActionPerformed

    private void txtNuevoNombreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNuevoNombreActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNuevoNombreActionPerformed

    private void rbUsuarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbUsuarioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbUsuarioActionPerformed

    private void rbAdminActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbAdminActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbAdminActionPerformed

    private void btnGuardarEstadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarEstadoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnGuardarEstadoActionPerformed

    private void btnCrearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCrearActionPerformed

        if (!rbAdmin.isSelected()) {
            mostrarMensajeUsuario("Solo el modo Administrador puede crear.");
            return;
        }

        String tipo = (String) comboTipoObjeto.getSelectedItem(); // "Archivo" o "Directorio"
        String nombre = txtNombre.getText().trim();
        int tamBloques = (Integer) spinTamanoBloques.getValue();

        if (nombre.isEmpty()) {
            mostrarMensajeUsuario("Debe ingresar un nombre.");
            return;
        }

        // 1) Verificar que haya un directorio seleccionado en el JTree
        javax.swing.tree.TreePath path = arbolSistema.getSelectionPath();
        if (path == null) {
            mostrarMensajeUsuario("Debe seleccionar un directorio en el árbol para crear el objeto.");
            return;
        }

        javax.swing.tree.DefaultMutableTreeNode nodoSel =
                (javax.swing.tree.DefaultMutableTreeNode) path.getLastPathComponent();
        Object objSel = nodoSel.getUserObject();

        if (!(objSel instanceof Directorio)) {
            mostrarMensajeUsuario("Debe seleccionar un directorio (no un archivo) para crear el objeto.");
            return;
        }

        Directorio dirDestino = (Directorio) objSel;
        String rutaDirectorio = construirRutaDirectorio(dirDestino);

        String rutaCompleta = rutaDirectorio.endsWith("/") ?
                            rutaDirectorio + nombre :
                            rutaDirectorio + "/" + nombre;

        if ("Archivo".equals(tipo)) {
            // validar que no exista ya ese nombre en este directorio
            if (existeNombreEnDirectorio(dirDestino, nombre)) {
                mostrarMensajeUsuario("Ya existe un archivo o directorio con ese nombre en esta ruta.");
                return;
            }

            // pista la calculas dentro de GestorProcesoES; aquí mando 0
            gestorProcesoES.crearProcesoCrear(rutaCompleta, 0, tamBloques);
            logSistema("Se creó proceso CREAR para archivo " + rutaCompleta);

        } else { // Directorio
            if (existeNombreEnDirectorio(dirDestino, nombre)) {
                mostrarMensajeUsuario("Ya existe un archivo o directorio con ese nombre en esta ruta.");
                return;
            }

            sistemaArchivos.crearDirectorio(rutaDirectorio, nombre);
            logSistema("Directorio creado directamente: " + rutaCompleta);
        }

        // actualizar vista
        actualizarTablaProcesos();
        refrescarArbol();
        refrescarTablaAsignacion();
        refrescarProcesosYDisco();
                      
    }//GEN-LAST:event_btnCrearActionPerformed

    private void btnIniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIniciarActionPerformed
        if (timerSimulacion != null && !timerSimulacion.isRunning()) {
            timerSimulacion.start();
            logSistema("Simulación iniciada.");
        }
    }//GEN-LAST:event_btnIniciarActionPerformed

    private void btnPausarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPausarActionPerformed
        if (timerSimulacion != null && timerSimulacion.isRunning()) {
            timerSimulacion.stop();
            logSistema("Simulación pausada.");
        }
    }//GEN-LAST:event_btnPausarActionPerformed

    private void btnReiniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReiniciarActionPerformed
        // TODO add your handling code here:
        inicializarModelo();
        refrescarArbol();
        refrescarTablaAsignacion();
        inicializarDiscoVisual();
        actualizarTablasProcesos();
        txtLogSistema.setText("");
        txtLogSistema.append("Sistema reiniciado.\n");
    }//GEN-LAST:event_btnReiniciarActionPerformed

    private void btnLeerSeleccionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLeerSeleccionActionPerformed
        if (archivoSeleccionado == null) {
            mostrarMensajeUsuario("Debe seleccionar un archivo para leer.");
            return;
        }

        String rutaArchivo = lblRutaSeleccionada.getText();
        int pista = 0;
        gestorProcesoES.crearProcesoLeer(rutaArchivo, pista);
        logSistema("Se creó proceso LEER para " + rutaArchivo + "\n");

        actualizarTablasProcesos();
    }//GEN-LAST:event_btnLeerSeleccionActionPerformed

    private void btnRenombrarSeleccionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRenombrarSeleccionActionPerformed
        if (!rbAdmin.isSelected()) {
            mostrarMensajeUsuario("Solo el modo Administrador puede renombrar.");
            return;
        }

        String nuevoNombre = txtNuevoNombre.getText().trim();
        if (nuevoNombre.isEmpty()) {
            mostrarMensajeUsuario("Debe indicar el nuevo nombre.");
            return;
        }

        if (archivoSeleccionado != null) {
            // Archivo: usar proceso RENOMBRAR_ARCHIVO
            String rutaCompleta = lblRutaSeleccionada.getText();
            int ultimaBarra = rutaCompleta.lastIndexOf("/");
            String rutaDir = (ultimaBarra <= 0) ? "/" : rutaCompleta.substring(0, ultimaBarra);
            String nombreViejo = archivoSeleccionado.getNombre();

            int pista = 0;
            gestorProcesoES.crearProcesoRenombrarArchivo(rutaDir, nombreViejo, nuevoNombre, pista);
            logSistema("Se creó proceso RENOMBRAR_ARCHIVO: " + nombreViejo + " -> " + nuevoNombre);

        } else if (directorioSeleccionado != null && directorioSeleccionado.getPadre() != null) {
            // Directorio (no root): proceso RENOMBRAR_DIRECTORIO
            String rutaPadre = construirRutaDirectorio(directorioSeleccionado.getPadre());
            String nombreViejo = directorioSeleccionado.getNombre();
            int pista = 0;
            gestorProcesoES.crearProcesoRenombrarDirectorio(rutaPadre, nombreViejo, nuevoNombre, pista);
            logSistema("Se creó proceso RENOMBRAR_DIRECTORIO: " + nombreViejo + " -> " + nuevoNombre);
        } else {
            mostrarMensajeUsuario("Nada seleccionado para renombrar.");
            return;
        }
        actualizarTablasProcesos();
    }//GEN-LAST:event_btnRenombrarSeleccionActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTree arbolSistema;
    private javax.swing.JButton btnCargar;
    private javax.swing.JButton btnCrear;
    private javax.swing.JButton btnEliminarSeleccion;
    private javax.swing.JButton btnGuardarEstado;
    private javax.swing.JButton btnIniciar;
    private javax.swing.JButton btnLeerSeleccion;
    private javax.swing.JButton btnPausar;
    private javax.swing.JButton btnReiniciar;
    private javax.swing.JButton btnRenombrarSeleccion;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JComboBox<String> comboPolitica;
    private javax.swing.JComboBox<String> comboTipoObjeto;
    private javax.swing.ButtonGroup grupoModo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTable jTableBloqueados;
    private javax.swing.JTable jTableEjecutados;
    private javax.swing.JTable jTableListos;
    private javax.swing.JTable jTableNuevos;
    private javax.swing.JTable jTableTerminados;
    private javax.swing.JLabel lblRutaSeleccionada;
    private javax.swing.JLabel lblSeleccionadoActual;
    private javax.swing.JLabel lblTamanoSeleccionado;
    private javax.swing.JLabel lblTipoSeleccionado;
    private javax.swing.JPanel panelArbol;
    private javax.swing.JPanel panelArchivoTexto;
    private javax.swing.JPanel panelArchivoTexto1;
    private javax.swing.JPanel panelBotonInicio;
    private javax.swing.JPanel panelCrear;
    private javax.swing.JPanel panelDisco;
    private javax.swing.JPanel panelDisco1;
    private javax.swing.JPanel panelOperarSeleccion;
    private javax.swing.JPanel panelTablaAsignacion;
    private javax.swing.JPanel panelTablaProcesos10;
    private javax.swing.JPanel panelTablaProcesos11;
    private javax.swing.JPanel panelTablaProcesos12;
    private javax.swing.JPanel panelTablaProcesos13;
    private javax.swing.JPanel panelTablaProcesos7;
    private javax.swing.JPanel panelZonaArbol;
    private javax.swing.JRadioButton rbAdmin;
    private javax.swing.JRadioButton rbUsuario;
    private javax.swing.JSpinner spinTamanoBloques;
    private javax.swing.JTable tablaAsignacion;
    private javax.swing.JScrollPane tablaProcesos10;
    private javax.swing.JScrollPane tablaProcesos11;
    private javax.swing.JScrollPane tablaProcesos12;
    private javax.swing.JScrollPane tablaProcesos13;
    private javax.swing.JScrollPane tablaProcesos7;
    private javax.swing.JTextArea txtLogSistema;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtNuevoNombre;
    // End of variables declaration//GEN-END:variables
}
