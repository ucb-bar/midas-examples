export SYNOPSYS_ROOT="<your sysnopsys tool root directory>"
export SYNOPSYS_RM_DIR="<path to your Synopsys Reference Methodology script tarball>"
# We expect these tarballs for the SAED32 directory
# - SAED32_EDK_OPEN_ACCESS_LIBS_07222015.tar.gz
# - SAED_EDK32.28nm_CORE_HVT_v_01132015.tar.gz
# - SAED_EDK32.28nm_CORE_LVT_v_01132015.tar.gz
# - SAED_EDK32.28nm_CORE_RVT_v_01132015.tar.gz
# - SAED_EDK32.28nm_IO_v_01132015.tar.gz
# - SAED_EDK32.28nm_PLL_v_01132015.tar.gz
# - SAED_EDK32.28nm_REF_v_01132015.tar.gz
# - SAED_EDK32.28nm_SRAM_v_01132015.tar.gz
# - SAED_EDK32.28nm_TECH_v_01132015.tar.gz
export SAED_TARBALL_DIR="<your SAED32 technology tarball directory>"
# We assume Design Compiler is installed at $SYNOPSYS_ROOT/syn/$DC_VERSION
export DC_VERSION="<your Design Compiler version>"
# We assume IC Compiler is installed at $SYNOPSYS_ROOT/icc/$ICC_VERSION
export ICC_VERSION="<your IC Compiler version>"
# We assume Formality is installed at $SYNOPSYS_ROOT/fm/$FORMALITY_VERSION
export FORMALITY_VERSION="<your Formality version>"
# We assume PrimeTime PX is installed at $SYNOPSYS_ROOT/pts/$PRIMETIME_POWER_VERSION
export PRIMETIME_VERSION="<your PrimeTime version>"
# We assume VCS is installed at $SYNOPSYS_ROOT/vcs/$VCS_VERSION
export VCS_VERSION="<your VCS version>"
# You're supposed to know where are these license files
export MGLS_LICENSE_FILE="<your license path>"
export SNPSLMD_LICENSE_FILE="<your license path>"
# Set this variable if you want to share HAMMER tools across multiple projects
export OBJ_TOOLS_DIR=
