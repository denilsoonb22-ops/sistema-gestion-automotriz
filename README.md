Sistema de Gestión Automotriz - Android Nativo (Kotlin)
Descripción del Proyecto
Este es un proyecto personal desarrollado para la administración financiera y operativa de servicios en talleres automotrices. El sistema permite el seguimiento de órdenes de trabajo, gestión de inventarios y generación de reportes técnicos detallados.

Tecnologías y Herramientas
Lenguaje: Kotlin Nativo.

Backend y Base de Datos: Firebase Firestore para la sincronización de datos en tiempo real.

Autenticación: Firebase Auth para el manejo seguro de sesiones de usuario.

Generación de Documentos: Uso de Canvas API y PdfDocument para la creación de reportes financieros y órdenes de servicio en formato PDF.

Arquitectura: Implementación de Corrutinas para procesos en segundo plano y manejo de Scoped Storage.

Desafíos de Ingeniería Resueltos
Algoritmo de Reporteo: Desarrollo de una solución personalizada con Canvas API para renderizar tablas y gráficos directamente en un PDF, eliminando la dependencia de librerías externas pesadas.

Sincronización de Datos: Configuración de persistencia de datos local y remota mediante Firebase, garantizando que el usuario no pierda información incluso sin conexión.

Interfaz UX/UI: Aplicación de lineamientos de Material Design para asegurar que la interfaz sea intuitiva y eficiente para operarios de taller.
