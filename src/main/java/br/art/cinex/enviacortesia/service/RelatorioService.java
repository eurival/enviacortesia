package br.art.cinex.enviacortesia.service;

 
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import br.art.cinex.enviacortesia.domain.Cortesia;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleGraphics2DExporterOutput;
import net.sf.jasperreports.export.SimpleGraphics2DReportConfiguration;

@Service
public class RelatorioService {

    private static final Logger logger = LoggerFactory.getLogger(RelatorioService.class);
    
    private static final String CORTESIA_JASPER_PATH = "relatorios/cortesia.jasper";
    private static final String CORTESIA_IMAGE_PATH = "relatorios/cortesia.jpeg";
    private static final float SCALE_FACTOR = 5.0f;

    /**
     * Gera relatório em formato PDF
     */
    public ByteArrayOutputStream gerarRelatorioPDF(List<Cortesia> cortesias) throws JRException, IOException {
        logger.info("Gerando relatório PDF para {} cortesias", cortesias.size());
        
        try (InputStream reportStream = new ClassPathResource(CORTESIA_JASPER_PATH).getInputStream()) {
            File imageFile = criarArquivoImagemTemporario();
            
            try {
                JasperReport jasperReport = (JasperReport) JRLoader.loadObject(reportStream);
                JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(cortesias);
                
                Map<String, Object> parameters = criarParametrosRelatorio(imageFile);
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JasperExportManager.exportReportToPdfStream(jasperPrint, baos);
                
                logger.info("Relatório PDF gerado com sucesso - {} bytes", baos.size());
                return baos;
                
            } finally {
                // Limpar arquivo temporário
                if (imageFile != null && imageFile.exists()) {
                    boolean deleted = imageFile.delete();
                    logger.debug("Arquivo temporário removido: {}", deleted);
                }
            }
        }
    }

    /**
     * Gera relatório em formato ZIP com imagens JPG
     */
    public ByteArrayOutputStream gerarRelatorioZip(List<Cortesia> cortesias) throws JRException, IOException {
        logger.info("Gerando relatório ZIP para {} cortesias", cortesias.size());
        
        try (InputStream reportStream = new ClassPathResource(CORTESIA_JASPER_PATH).getInputStream()) {
            File imageFile = criarArquivoImagemTemporario();
            
            try {
                JasperReport jasperReport = (JasperReport) JRLoader.loadObject(reportStream);
                JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(cortesias);
                
                Map<String, Object> parameters = criarParametrosRelatorio(imageFile);
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
                
                ByteArrayOutputStream zipStream = exportarRelatorioParaZipEmMemoria(jasperPrint);
                
                logger.info("Relatório ZIP gerado com sucesso - {} bytes", zipStream.size());
                return zipStream;
                
            } finally {
                // Limpar arquivo temporário
                if (imageFile != null && imageFile.exists()) {
                    boolean deleted = imageFile.delete();
                    logger.debug("Arquivo temporário removido: {}", deleted);
                }
            }
        }
    }

    /**
     * Cria arquivo temporário da imagem do relatório
     */
    private File criarArquivoImagemTemporario() throws IOException {
        logger.debug("Criando arquivo temporário da imagem");
        
        try (InputStream imageStream = new ClassPathResource(CORTESIA_IMAGE_PATH).getInputStream()) {
            File imageFile = File.createTempFile("cortesia_", ".jpeg");
            imageFile.deleteOnExit(); // Garantir limpeza automática
            
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                byte[] buffer = new byte[8192]; // Buffer maior para melhor performance
                int bytesRead;
                while ((bytesRead = imageStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            logger.debug("Arquivo temporário criado: {}", imageFile.getAbsolutePath());
            return imageFile;
        }
    }

    /**
     * Cria parâmetros para o relatório JasperReports
     */
    private Map<String, Object> criarParametrosRelatorio(File imageFile) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_DIR", getReportDirectory());
        parameters.put("IMAGE_PATH", imageFile.getAbsolutePath());
        
        logger.debug("Parâmetros do relatório criados: REPORT_DIR={}, IMAGE_PATH={}", 
                    parameters.get("REPORT_DIR"), parameters.get("IMAGE_PATH"));
        
        return parameters;
    }

    /**
     * Obtém o diretório dos relatórios
     */
    private String getReportDirectory() {
        try {
            ClassPathResource resource = new ClassPathResource("relatorios/");
            if (resource.exists()) {
                return resource.getFile().getAbsolutePath() + "/";
            }
        } catch (IOException e) {
            logger.warn("Não foi possível obter o diretório físico de relatórios: {}", e.getMessage());
        }
        
        // Fallback para path relativo
        return "classpath:relatorios/";
    }

    /**
     * Exporta relatório para ZIP em memória com imagens de alta qualidade
     */
    private ByteArrayOutputStream exportarRelatorioParaZipEmMemoria(JasperPrint jasperPrint) 
            throws JRException, IOException {
        
        logger.debug("Iniciando exportação para ZIP - {} páginas", jasperPrint.getPages().size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Configurar compressão do ZIP
            zos.setLevel(ZipOutputStream.DEFLATED);
            
            JRGraphics2DExporter exporter = new JRGraphics2DExporter();

            // Processar cada página do relatório
            for (int pageIndex = 0; pageIndex < jasperPrint.getPages().size(); pageIndex++) {
                logger.debug("Processando página {} de {}", pageIndex + 1, jasperPrint.getPages().size());
                
                BufferedImage bufferedImage = criarImagemComAltaResolucao(jasperPrint);
                Graphics2D g2d = configurarGraphics2D(bufferedImage);

                try {
                    // Configurar exportador para a página específica
                    configurarExportadorParaPagina(exporter, jasperPrint, g2d, pageIndex);
                    
                    // Exportar página para o Graphics2D
                    exporter.exportReport();

                    // Converter imagem para bytes e adicionar ao ZIP
                    byte[] imageData = converterImagemParaBytes(bufferedImage);
                    adicionarImagemAoZip(zos, imageData, pageIndex + 1);
                    
                    logger.debug("Página {} processada - {} bytes", pageIndex + 1, imageData.length);

                } finally {
                    g2d.dispose();
                }
            }
            
            zos.finish();
        }

        logger.debug("Exportação ZIP concluída - {} bytes totais", baos.size());
        return baos;
    }

    /**
     * Cria imagem com alta resolução
     */
    private BufferedImage criarImagemComAltaResolucao(JasperPrint jasperPrint) {
        int width = (int) (jasperPrint.getPageWidth() * SCALE_FACTOR);
        int height = (int) (jasperPrint.getPageHeight() * SCALE_FACTOR);
        
        logger.debug("Criando imagem {}x{} (escala: {})", width, height, SCALE_FACTOR);
        
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Configura Graphics2D com qualidade máxima
     */
    private Graphics2D configurarGraphics2D(BufferedImage bufferedImage) {
        Graphics2D g2d = bufferedImage.createGraphics();

        // Aplicar escala para alta resolução
        g2d.scale(SCALE_FACTOR, SCALE_FACTOR);

        // Configurar renderização de alta qualidade
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        // Definir fundo branco
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

        return g2d;
    }

    /**
     * Configura o exportador JasperReports para uma página específica
     */
    private void configurarExportadorParaPagina(JRGraphics2DExporter exporter, 
                                               JasperPrint jasperPrint, 
                                               Graphics2D g2d, 
                                               int pageIndex) {
        // Configurar entrada
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

        // Configurar saída
        SimpleGraphics2DExporterOutput output = new SimpleGraphics2DExporterOutput();
        output.setGraphics2D(g2d);
        exporter.setExporterOutput(output);

        // Configurar página específica
        SimpleGraphics2DReportConfiguration configuration = new SimpleGraphics2DReportConfiguration();
        configuration.setPageIndex(pageIndex);
        exporter.setConfiguration(configuration);
    }

    /**
     * Converte BufferedImage para array de bytes em formato JPG
     */
    private byte[] converterImagemParaBytes(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream imageBaos = new ByteArrayOutputStream();
        
        // Usar JPG com qualidade máxima
        boolean success = ImageIO.write(bufferedImage, "jpg", imageBaos);
        if (!success) {
            throw new IOException("Falha ao converter imagem para JPG");
        }
        
        return imageBaos.toByteArray();
    }

    /**
     * Adiciona imagem ao arquivo ZIP
     */
    private void adicionarImagemAoZip(ZipOutputStream zos, byte[] imageData, int pageNumber) throws IOException {
        String fileName = String.format("cortesia_%03d.jpg", pageNumber);
        ZipEntry zipEntry = new ZipEntry(fileName);
        
        // Definir timestamp para consistência
        zipEntry.setTime(System.currentTimeMillis());
        
        zos.putNextEntry(zipEntry);
        zos.write(imageData);
        zos.closeEntry();
        
        logger.debug("Imagem adicionada ao ZIP: {} ({} bytes)", fileName, imageData.length);
    }

    /**
     * Valida se os recursos necessários existem
     */
    public boolean validarRecursos() {
        try {
            ClassPathResource jasperResource = new ClassPathResource(CORTESIA_JASPER_PATH);
            ClassPathResource imageResource = new ClassPathResource(CORTESIA_IMAGE_PATH);
            
            boolean jasperExists = jasperResource.exists();
            boolean imageExists = imageResource.exists();
            
            logger.info("Validação de recursos - Jasper: {}, Imagem: {}", jasperExists, imageExists);
            
            return jasperExists && imageExists;
            
        } catch (Exception e) {
            logger.error("Erro na validação de recursos: {}", e.getMessage(), e);
            return false;
        }
    }
}
