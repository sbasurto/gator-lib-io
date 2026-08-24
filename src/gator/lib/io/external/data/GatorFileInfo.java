/*
 * Copyright (C) 2018 Sergio Basurto Juarez
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package gator.lib.io.external.data;

/**
 *
 * <a href="mailto:sbasurto@soft-gator.com">Sergio Basurto Juárez</a>
 */
public class GatorFileInfo {
        private String fileName;
        private String confFile;
        private String cuentaId; 
        private String bodegaId; 
        private String usuario; 
        private String idctrl;
        private String sesionId;
        private String processType;
        public void setFileName(String _fileName) {
                fileName = _fileName;
        }
        public void setConfFile(String _confFile) {
                confFile = _confFile;
        }
        public void setAccount(String _account) {
                cuentaId = _account;
        }
        public void setWarehouse(String _warehouse) {
                bodegaId = _warehouse;
        }
        public void setUser(String _user) {
                usuario = _user;
        }
        public void setIdctrl(String _idctrl) {
                idctrl = _idctrl;
        }
        public void setSessionId(String _sesionId) {
                sesionId = _sesionId;
        }
        public void setProcessType(String _processType) {
                processType = _processType;
        }
        public String getFileName() {
                return fileName;
        }
        public String getConfFile() {
                return confFile;
        }
        public String getAccount() {
                return cuentaId;
        }        
        public String getWarehouse() {
                return bodegaId;
        }
        public String getUser() {
                return usuario;
        }
        public String getIdctrl() {
                return idctrl;
        }
        public String getSeionId() {
                return sesionId;
        }
        public String getProcessType() {
                return processType;
        }
}
