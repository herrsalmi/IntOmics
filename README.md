<!-- badges: start -->
[![Generic badge](https://img.shields.io/badge/version-0.9--alpha.2-green)](https://shields.io/)
[![License: GPL v3](https://img.shields.io/badge/license-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
<!--badges: end -->
## Multi omics data integration tool
IntOmics is a tool for integrating proteomics and transcriptomics human data in order to uncover cell crosstalk mechanisms.


## Usage
```
java -jar intOmics.jar -p <file> -a <file> -g <file> [options]*
```

## Arguments

| Option             | Description                                                                 |
|:-------------------|:----------------------------------------------------------------------------|
| `-p <file>`        | Text file containing secreted proteins                                      |
| `-a <file>`        | Text file containing all expressed genes                                    |
| `-g <file>`        | Text file containing differentially expressed genes                         |
| `-f <string>`      | Output format: TSV or FWF. Default: TSV                                     |
| `-db <string>`     | Pathway database: [KEGG, WIKIPATHWAYS, REACTOME]. Default: WIKIPATHWAYS     |
| `-s <int>`         | Minimum score for PPI (range from 0 to 1000). Default: 900                  |
| `-fc <double>`     | Fold change cutoff. Default: 1.5                                            |
| `-pv <double>`     | P-value cutoff. Default: 0.05                                               |
| `-t <int>`         | Number of threads to use. Default: 4                                        |
| `-d <string>`      | Custom separator for input files: Default: ';'                              |
| `--no-cached-sets` | Pull an up-to-date list of pathways                                         |
| `--no-cached-ppi`  | Disable usage of cached PPI data                                            |
| `-h`               | Print the help screen                                                       |


## Input files
Input files should be in CSV format and can have a header line starting with `#`. The default column separator is `;`, but a different one can be specified using option `-d`.

##### Secreted proteins
Text file containing protein names or corresponding Entrez gene id, each one on a separate line.
##### Expressed genes
Text file containing symbols for all expressed genes, each one on a separate line. This list is used to infer membrane protein-coding genes.
##### Differential expression testing results
Text file in CSV format with three columns: `gene name`, `p value` and `fold change`. 


## Outputs
There are two main output files:
* A text file either in TSV or FWF format containing:
    * **Protein**: secreted proteins symbol.
    * **Protein description**: full name of the protein.
    * **Gene**: symbol corresponding to membrane protein-coding gene.
    * **Gene description**: full name of the gene.
    * **I score**: interaction score between the protein and the receptor.
    * **Pathways**: list of pathways with an enrichment score and a p-value.
* An HTML file representing the network of interactions between secreted proteins and cell receptors.
    
## Gene set enrichment analysis
The GSEA implemented in this tool is slightly different than the on proposed by Subramanian et al. (2005).

Gene sets are defined as pathways from either `KEGG`, `WIKIPATHWAYS` or `REACTOME`. `WIKIPATHWAYS` is chosen by default if option `-db` is not specified. 
This tool has prebuilt sets for `WIKIPATHWAYS` and `KEGG`, but an up-to-date version can be rebuilt by using option `--no-cached-sets` and stored in `sets/` folder for future use.
Note that if no new pathways exist, the prebuilt version will be used.
This argument though has no effect when using `REACTOME` as no prebuilt sets are available, and the online service is always queried.
 
## Protein-protein interactions
Protein-protein interactions data from StringDB is used to establish a link between secreted proteins and surface receptors.
Interaction scores rank from 0 to 1000, and they do not indicate the strength or the specificity of the interaction.
Instead, they are indicators of confidence. A score of 500 would indicate that roughly every second interaction might be erroneous (i.e., a false positive).

| score              | confidence              |
|:-------------------|:------------------------|
| x > 900            | highest confidence      |
| x > 700            | high confidence         |
| x > 400            | medium confidence       |
| x > 150            | low confidence          |

A cached network of PPI is used when the interaction score threshold is greater than 700.
You can override this behavior by using option `--no-cached-ppi`.







