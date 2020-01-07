package org.pmoi;

import org.pmoi.business.GeneOntologyMapper;
import org.pmoi.business.NCBIQueryClient;
import org.pmoi.handler.HttpConnector;
import org.pmoi.models.Gene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MainEntry {

    private static final String VERSION = "0.1";
    public static final boolean USE_GENE_NAME = true;
    private static final String PROG_NAME = "IntOmics";
    public static final int MAX_TRIES = 100;
    public static final String NCBI_API_KEY = "40065544fb6667a5a723b649063fbe596e08";

    public MainEntry() {
        run();
    }

    private void run() {
        HttpConnector connector = new HttpConnector();
        NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
        GeneOntologyMapper goMapper = new GeneOntologyMapper();
        try {
            goMapper.load("gene2go");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //List<String> genes = Arrays.asList("CNGA1","TANGO2","MTHFD2","PPIA","LINS1","GCNT1","MAP1S","NTRK3-AS1","LINC01530","SNORD103A","EHMT2","ZBTB7A","BAIAP2","ZNF425","BSG","NUMBL","NOD2","QTRT2","PRTG","LOXL1","LRRC28","C7orf60","LSP1","PTCSC1","FAM192A","RAD1","NANOS1","BACH2","IGHV1-18","ELMO2","NR2F2-AS1","PVR","ADAMTS5","ENO1-IT1","NRAV","ZW10","PCDHA8","RANBP17","EML2","KLC3","SCP2D1","BRINP1","ESD","ZNF852","CASP8AP2","SYT17","FAM69C","EPB42","MBOAT2","S100A12","PABPC4L","PFKM","TP53TG3C","ADAM30","C6orf118","LINC00540","IFNA16","TKFC","RNF150","OTOGL","NDUFS6","FAM25E","ATG5","MATR3","KCNK3","TTLL6","DPCD","SALRNA2","LARGE-AS1","LGALS4","ODF3L1","CKAP5","NSFL1C","PLCH1-AS1","SNORD115-27","TTC8","PPP1R37","RGS3","MTHFD2L","TLDC2","SCAF8","PCNA-AS1","C1GALT1","H2AFJ","CALML3","EGLN1","EPN2-AS1","BCKDHA","HAR1A","SIPA1L2","FAM98C","SLC34A2","MAPK8IP2","NMTRQ-TTG14-1","NUCKS1","RNA5S2","TCF4-AS1","ZNF701","CDSN","ZNF324","SPEN","ITGA1","HNF1B","C19orf68","MFAP1","HOPX","HORMAD2","NOXA1","ASB15","KANSL3","TRAPPC8","KANK2","MFAP4","ADGRA1","PKD1","ZMYND10-AS1","CARF","DFNB90","POTEH-AS1","GOLGA6L10","KIAA0556","CSN2","RPS6KA4","DLG5-AS1","PEX5","EIF2B5","IL1RL1","FAM195A","ITPR3","S100B","ALS2CL","XPC","SNORD32B","HLA-F","RUSC2","ZNF713","PLEKHM2","TRP-AGG6-1","CTDSPL","NAA40","CNTFR","LINC00963","ERVK-13","CNNM2","SCYL1","OR9G9","ZFPM2","ANP32B","LINC00115","AHSA1","C8orf44","DUX4L18","FRA5D","ARID1B","PPP2R5A","SNORD115-9","IGHV4-59","LINGO3","ZFP42","MAP2K7","LINC00473","SLC26A11","RBM42","RC3H2","CDK17","LINC01481","CHCHD7","DYNAP","GALNT15","POC1B","DDX24","PLXNB2","LINC01411","TMEM30A","INF2","TMEM176A","LINC01237","MYEF2","MT4","GPER1","KLHDC7A","SCN2B","AVEN","CEBPG","CARTPT","CLSPN","PLD4","ERICD","LINC01606","ALDH1A1","LINC01162","GDE1","ARFGEF3","OR2A12","VCAN","JMJD4","OR8I2","UGGT2","RAB3GAP2","SMYD3-IT1","TCTN2","CDH1","ZNF75A","FOSB","OPRK1","PGAM2","SPATA31D1","DMWD","LRRFIP1","P4HA3","IGLV5-48","FCN1","DDX41","LINC01550","SPTY2D1","PAMR1","PROC","DCAF7","EIF1","ROPN1B","ZBTB8B","PLCZ1","CXCL10","WS2B","BANF1","ATP1A1-AS1","LINC01386","RNA5S7","TAAR1","CYP3A43","SLC35C2","ARHGAP9","PRRT4","CA7","TRV-TAC3-1","NDST3","ZNF781","CRTAM","SMKR1","C2orf72","BOK","IDH1-AS1","TRP-AGG5-1","ADCY10","ATP5J2","OR11A1","NPHP3-ACAD11","NFRKB","ESA4","PCAP","ZNF433","OR4D1","SMIM2-IT1","TMEM9","TARBP1","SMCO1","FKBP6","KIAA2013","CCR4","SPATA31D3","TP53BP2","GAS2","CCDC188","ZDHHC1","TRA-AGC12-3","ATE1-AS1","ALS7","PCGF3","SLC25A4","SPDYE16","PGAP3","N4BP2L2","PRR20A","BRAF","LRCH4","KIAA1671","RFX3-AS1","ENPP3","SLBP","DUSP19","OR14J1","RPEL1","CSRNP2","MAST4-IT1","PEA15","C1QL2","MRAS","MAL","DTWD2","FSD1","USF1","OR4N4","TMEM108-AS1","AKT3-IT1","AQP12B","LINC00440","FSIP2","FLNB","RSL1D1","CTR9","JPH2","POTEB","RGS14","MCM8-AS1","L3MBTL2","SNORD166","HOXC9","STX17-AS1","LINC00938","MAB21L3","SLC13A1","KRTAP9-4","ENO1-AS1","UNC119","PREX1","LINC00665","KIRREL-IT1","ERVFRD-1","ATF7","THUMPD3-AS1","CCDC91","AQP11","MCM7","KRTAP20-3","PTAFR","RNF5","SCAMP1-AS1","RECQL","CNKSR3","NLRP11","PROX1-AS1","CCDC148","T","KRTAP10-11","TRA-TGC6-1","LINC01019","TBC1D30","SNORA95","ICAM1","MYO1F","ANKRD7","STX18","CMTM8","LAMB3","RNF39","ATOH7","JAKMIP3","APOA4","ZNF783","PKP1","ZMYM4-AS1","HOXA13","TDRD12","LIPJ","UBAP2L","LINC00903","HIST1H4E","SKAP1","TIA1","OR10H4","TRBV7-6","ACSL6","UBE4A","FRA19A","IGKV6D-21","LINC01102","GRM8","TRC-GCA9-2","SNORD49B","FMNL3","ZNF484","SNORA112","AK6","TF","RIOK2","GM2A","PAQR6","SPDYC","MFSD4A","LINC00858","CKLF","TRF-GAA8-1","MIIP","LUM","RPS28","INO80B-WBP1","HOXA10-AS","BCO1","TOMM40L","SIX3","CLEC10A","GAS2L2","FFAR3","SLC25A42","ITGA2","PARP11","LNP1","TIGAR","KCNA5","C1orf116","C1orf229","AQP9","TRC-GCA2-1","KCNIP3","TUSC5","DFNB57","AKR1E2","LNX1-AS1","FKBP1B","SUN3","SELPLG","ERLIN1","CSMD2","CDKN1A","NPY2R","ANKUB1","BAIAP3","PIWIL1","DNASE1L2","DUSP26","FRA9A","SNORD115-14","FLT3","CKMT1A","HIST1H2BM","TBL1XR1","IMPA1","TRG-CCC3-1","KIF9","FAM24B","NPIPA8","STRADA","DUSP4","LIN28A","GDPD4","TPRG1-AS1","CCDC151","HPP1","HS3ST2","LINC00276","PCDHGA8","GMDS","CRTC3-AS1","FRMD6-AS1","PLCH2","SAX1","KCNC4","CCDC180","GYS2","MYO16","STX16-NPEPL1","CLN8","PPIL4","GAL3ST4","CCDC71L","IGSF23","METTL12","TRBJ1-4","NPTN-IT1","EGFL8","RNMT","CRACR2A","RBMY1F","IGKV1-6","TRAV8-4","OR5D16","TOLLIP","IGLV1-47","PDE6B","CDH2","ATPAF1","NPHP1","ADCK2","NSMCE4A","WBP2NL","PRR11","DUX4L22","TSPAN10","PHACTR2","AP1G1","PKDREJ","MTO1","ZNF446","ZNHIT3","PRR27","ADAM11","RAMP2-AS1","CAB39","RNASE11","TMEM192","THOC3","GSTM1","LINC01276","CCL4","LINC00685","ZNF385D","LRG1","C18orf12","DIAPH3-AS1","HEXB");
        //List<Gene> inputGenes = genes.stream().map(Gene::new).collect(Collectors.toList());

        List<Gene> inputGenes = readGeneFile("genes.txt");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<String>> future = new ArrayList<>(1000);
        AtomicInteger index = new AtomicInteger(0);
        Callable<String> callable = () -> ncbiQueryClient.geneNameToEntrezID(inputGenes.get(index.getAndIncrement()));

        assert inputGenes != null;
        inputGenes.forEach(g -> {
            future.add(executor.submit(callable));
        });

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        future.parallelStream().map(e -> {
//            try {
//                return e.get();
//            } catch (InterruptedException | ExecutionException ex) {
//                ex.printStackTrace();
//            }
//            return null;
//        }).filter(goMapper::checkGO).forEach(System.out::println);

        // if a gene has no EntrezID it will also get removed here
        inputGenes.parallelStream()
                .filter(g -> g.getGeneEntrezID() != null && !g.getGeneEntrezID().isEmpty())
                .filter(e -> goMapper.checkGO(e.getGeneEntrezID()))
                .forEach(System.out::println);
    }

    private List<Gene> readGeneFile(String filePath) {
        try {
            return Files.lines(Path.of(filePath))
                    .filter(Predicate.not(String::isBlank))
                    .distinct()
                    .map(Gene::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        new MainEntry();
    }
}
