 #!/bin/bash
 #Copyright © 1998-2024. Scientific Lab. Gamma Technologies. All rights reserved.
          
 case "$1" in
  install)
          
          echo "Installing TumarCSP v.5.2.15.051 for linux64...START"
          
          mkdir /TumarCSP
          mkdir /TumarCSP/bin
          mkdir /TumarCSP/etc
          mkdir /TumarCSP/etc/lic
          mkdir /TumarCSP/etc/lic_r
          
          touch           /TumarCSP/etc/cptumar.conf
          chmod 666       /TumarCSP/etc/cptumar.conf
          chown root:root /TumarCSP/etc/cptumar.conf
          
          echo "[profiles]"                       >>   /TumarCSP/etc/cptumar.conf
          echo "FSystem=file://TEST@//home/user"  >>   /TumarCSP/etc/cptumar.conf
          mkdir /TumarCSP/lib
          mkdir /TumarCSP/lib32
          
          chmod 755 /TumarCSP
          chmod 755 /TumarCSP/bin
          chmod 777 /TumarCSP/etc
          chmod 755 /TumarCSP/etc/lic
          chmod 755 /TumarCSP/etc/lic_r
          chmod 755 /TumarCSP/lib
          chmod 755 /TumarCSP/lib32
          
          cp lic/libcsp-plg-jtoken.so.1.0.0.reg     /TumarCSP/etc
          cp lic/NPCKCLIENT32.2.reg /TumarCSP/etc/lic
          cp lic/NPCKCLIENT64.2.reg /TumarCSP/etc/lic
          
          cp lic/NPCKCLIENT32_R.2.reg /TumarCSP/etc/lic_r
          cp lic/NPCKCLIENT64_R.2.reg /TumarCSP/etc/lic_r
          
          chmod 644 /TumarCSP/etc/lic/*
          chmod 644 /TumarCSP/etc/lic_r/*
          
          cp bin/cputil                       /TumarCSP/bin
          cp lib/libcsp-plg-jtoken.so.1.0.0        /TumarCSP/lib
          cp lib/libcertex-csp.so.v.5.2.15.051     /TumarCSP/lib
          cp lib/libcertex-csp_r.so.v.5.2.15.051   /TumarCSP/lib
          cp lib32/libcertex-csp.so.v.5.2.15.051   /TumarCSP/lib32
          cp lib32/libcertex-csp_r.so.v.5.2.15.051 /TumarCSP/lib32
          
          chmod 755 /TumarCSP/lib/*
          chmod 755 /TumarCSP/lib32/*
          
          ln -sf /TumarCSP/lib/libcertex-csp.so.v.5.2.15.051     /TumarCSP/lib/libcptumar.4.0.so
          ln -sf /TumarCSP/lib/libcertex-csp_r.so.v.5.2.15.051   /TumarCSP/lib/libcptumar_r.4.0.so
          ln -sf /TumarCSP/lib32/libcertex-csp.so.v.5.2.15.051   /TumarCSP/lib32/libcptumar.4.0.so
          ln -sf /TumarCSP/lib32/libcertex-csp_r.so.v.5.2.15.051 /TumarCSP/lib32/libcptumar_r.4.0.so
          ln -sf /TumarCSP/bin/cputil /usr/bin/cputil
          
          ln -sf /TumarCSP/lib/libcptumar.4.0.so       /lib64/libcptumar.so.4.0
          ln -sf /TumarCSP/lib/libcptumar_r.4.0.so     /lib64/libcptumar_r.so.4.0
          ln -sf /TumarCSP/lib32/libcptumar.4.0.so     /lib/libcptumar.so.4.0
          ln -sf /TumarCSP/lib32/libcptumar_r.4.0.so   /lib/libcptumar_r.so.4.0
          
          cp -rf doc/cputil.1.gz                       /usr/share/man/man1
          chmod 644 /usr/share/man/man1/cputil.1.gz
          echo "Installing TumarCSP v.5.2.15.051 for linux64...STOP"
  ;;
          
  update)
          
          BKPNAME="csp_bkp_`date '+%Y%m%d_%H%M%S'`";
              echo "Backup old data...START"
              tar cvfP $BKPNAME.tar /TumarCSP
              echo "Backup old data...STOP"
              sleep 2
              echo "          "
          
          echo "Updating TumarCSP v.5.2.15.051 for linux64...START"
          
          rm -rf /TumarCSP/bin
          rm -rf /TumarCSP/etc/*.reg
          rm -rf /TumarCSP/etc/lic
          rm -rf /TumarCSP/etc/lic_r
          rm -rf /TumarCSP/lib
          rm -rf /TumarCSP/lib32
          rm -rf /lib64/libcptumar*
          rm -rf /lib/libcptumar*
          
          mkdir /TumarCSP/bin
          mkdir /TumarCSP/etc/lic
          mkdir /TumarCSP/etc/lic_r
          mkdir /TumarCSP/lib
          mkdir /TumarCSP/lib32
          
               if ! [ -e /TumarCSP/etc/cptumar.conf ]
                    then
                        touch           /TumarCSP/etc/cptumar.conf
                        chmod 666       /TumarCSP/etc/cptumar.conf
                        chown root:root /TumarCSP/etc/cptumar.conf
                        echo "[profiles]"                       >>   /TumarCSP/etc/cptumar.conf
                        echo "FSystem=file://TEST@//home/user"  >>   /TumarCSP/etc/cptumar.conf
               fi
          
          chmod 755 /TumarCSP
          chmod 755 /TumarCSP/bin
          chmod 777 /TumarCSP/etc
          chmod 666 /TumarCSP/etc/cptumar.conf
          chmod 755 /TumarCSP/etc/lic
          chmod 755 /TumarCSP/etc/lic_r
          chmod 755 /TumarCSP/lib
          chmod 755 /TumarCSP/lib32
          
          cp -rf lic/libcsp-plg-jtoken.so.1.0.0.reg     /TumarCSP/etc
          cp lic/NPCKCLIENT32.2.reg /TumarCSP/etc/lic
          cp lic/NPCKCLIENT64.2.reg /TumarCSP/etc/lic
          cp lic/NPCKCLIENT32_R.2.reg /TumarCSP/etc/lic_r
          cp lic/NPCKCLIENT64_R.2.reg /TumarCSP/etc/lic_r
          chmod 644 /TumarCSP/etc/lic/*
          chmod 644 /TumarCSP/etc/lic_r/*
          
          cp bin/cputil                       /TumarCSP/bin
          cp -rf lib/libcsp-plg-jtoken.so.1.0.0    /TumarCSP/lib
          cp lib/libcertex-csp.so.v.5.2.15.051     /TumarCSP/lib
          cp lib/libcertex-csp_r.so.v.5.2.15.051   /TumarCSP/lib
          cp lib32/libcertex-csp.so.v.5.2.15.051   /TumarCSP/lib32
          cp lib32/libcertex-csp_r.so.v.5.2.15.051 /TumarCSP/lib32
          chmod 755 /TumarCSP/lib/*
          chmod 755 /TumarCSP/lib32/*
          
          ln -sf /TumarCSP/lib/libcertex-csp.so.v.5.2.15.051     /TumarCSP/lib/libcptumar.4.0.so
          ln -sf /TumarCSP/lib/libcertex-csp_r.so.v.5.2.15.051   /TumarCSP/lib/libcptumar_r.4.0.so
          ln -sf /TumarCSP/lib32/libcertex-csp.so.v.5.2.15.051   /TumarCSP/lib32/libcptumar.4.0.so
          ln -sf /TumarCSP/lib32/libcertex-csp_r.so.v.5.2.15.051 /TumarCSP/lib32/libcptumar_r.4.0.so
          ln -sf /TumarCSP/bin/cputil /usr/bin/cputil
          
          ln -sf /TumarCSP/lib/libcptumar.4.0.so       /lib64/libcptumar.so.4.0
          ln -sf /TumarCSP/lib/libcptumar_r.4.0.so     /lib64/libcptumar_r.so.4.0
          ln -sf /TumarCSP/lib32/libcptumar.4.0.so     /lib/libcptumar.so.4.0
          ln -sf /TumarCSP/lib32/libcptumar_r.4.0.so   /lib/libcptumar_r.so.4.0
          
          cp -rf doc/cputil.1.gz                       /usr/share/man/man1
          chmod 644 /usr/share/man/man1/cputil.1.gz
          echo "Updating TumarCSP v.5.2.15.051 for linux64...STOP"
          
  ;;
          
  uninstall)
          
          echo -n "Are you sure you want to uninstall TumarCSP v.5.0.15.051? (y/n): "
                  read key
                       if [ "$key" = "y" ] 
                            then
                                echo "Uninstalling TumarCSP v.5.2.15.051 for linux64...START"
                                       rm -rf /TumarCSP
                                       rm -rf /lib64/libcptumar*
                                       rm -rf /lib/libcptumar*
                                       rm -rf /usr/share/man/man1/cputil.1.gz
                                       rm -rf /usr/bin/cputil
                                echo "Uninstalling TumarCSP v.5.2.15.051 for linux64...STOP"
                       elif [ "$key" = "n" ] 
                            then
                                echo "Uninstall TumarCSP v.5.2.15.051 aborted"
                       else
                            $0 uninstall
                       fi
  ;;
          
  *)
          echo "    "
          echo "TumarCSP v.5.2.15.051 for linux64"
          echo "WARNING!!! Before performing any operations, please, stop all applications that use TumarCSP"
          echo "    "
          echo "        Usage: ./setup_csp.sh [OPTIONS]"
          echo "                install       - Installing CSP on a clean system"
          echo "                update        - Backup current CSP, uninstall and install new CSP"
          echo "                uninstall     - Full uninstalling CSP"
          echo "    "
          
 exit 0
 esac
